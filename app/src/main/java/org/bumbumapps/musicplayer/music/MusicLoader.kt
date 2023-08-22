package org.bumbumapps.musicplayer.music

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.database.getStringOrNull
import androidx.core.text.isDigitsOnly
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.excluded.ExcludedDatabase
import org.bumbumapps.musicplayer.settings.SettingsManager

/**
 * This class acts as the base for most the black magic required to get a remotely sensible music
 * indexing system while still optimizing for time. I would recommend you leave this module now
 * before you lose your sanity trying to understand the hoops I had to jump through for this
 * system, but if you really want to stay, here's a debrief on why this code is so awful.
 *
 * MediaStore is not a good API. It is not even a bad API. Calling it a bad API is an insult to
 * other bad android APIs, like CoordinatorLayout or InputMethodManager. No. MediaStore is a
 * crime against humanity and probably a way to summon Zalgo if you look at it the wrong way.
 *
 * You think that if you wanted to query a song's genre from a media database, you could just
 * put "genre" in the query and it would return it, right? But not with MediaStore! No, that's
 * too straightfoward for this class that was dropped on it's head as a baby. So instead, you
 * have to query for each genre, query all the songs in each genre, and then iterate through those
 * songs to link every song with their genre. This is not documented anywhere, and the
 * O(mom im scared) algorithm you have to run to get it working single-handedly DOUBLES Auxio's
 * loading times. At no point have the devs considered that this column is absolutely insane, and
 * instead focused on adding infuriat- I mean nice proprietary extensions to MediaStore for their
 * own Google Play Music, and we all know how great that worked out!
 *
 * It's not even ergonomics that makes this API bad. It's base implementation is completely borked
 * as well. Did you know that MediaStore doesn't accept dates that aren't from ID3v2.3 MP3 files?
 * I sure didn't, until I decided to upgrade my music collection to ID3v2.4 and Xiph only to see
 * that their metadata parser has a brain aneurysm the moment it stumbles upon a dreaded TRDC or
 * DATE tag. Once again, this is because internally android uses an ancient in-house metadata
 * parser to get everything indexed, and so far they have not bothered to modernize this parser
 * or even switch it to something more powerful like Taglib, not even in Android 12. ID3v2.4 has
 * been around for 21 years. It can drink now. All of my what.
 *
 * Not to mention all the other infuriating quirks. Album artists can't be accessed from the albums
 * table, so we have to go for the less efficent "make a big query on all the songs lol" method
 * so that songs don't end up fragmented across artists. Pretty much every OEM has added some
 * extension or quirk to MediaStore that I cannot reproduce, with some OEMs (COUGHSAMSUNGCOUGH)
 * crippling the normal tables so that you're railroaded into their music app. The way I do
 * blacklisting relies on a deprecated method, and the supposedly "modern" method is SLOWER and
 * causes even more problems since I have to manage databases across version boundaries. Sometimes
 * music will have a deformed clone that I can't filter out, sometimes Genres will just break for no
 * reason, sometimes this spaghetti parser just completely falls apart and is unable to get any
 * metadata. Everything is broken in it's own special unique way and I absolutely hate it.
 *
 * Is there anything we can do about it? No. Google has routinely shut down issues that begged google
 * to fix glaring issues with MediaStore or to just take the API behind the woodshed and shoot it.
 * Largely because they have zero incentive to improve it, especially for such obscure things
 * as indexing music. As a result, some players like Vanilla and VLC just hack their own pidgin
 * version of MediaStore from their own parsers, but this is both infeasible for Auxio due to how
 * incredibly slow it is to get a file handle from the android sandbox AND how much harder it is
 * to manage a database of your own media that mirrors the filesystem perfectly. And even if I set
 * aside those crippling issues and changed my indexer to that, it would face the even larger
 * problem of how google keeps trying to kill the filesystem and force you into their
 * ContentResolver API. In the future MediaStore could be the only system we have, which is also the
 * day that greenland melts and birthdays stop happening forever.
 *
 * I'm pretty sure nothing is going to happen and MediaStore will continue to be neglected and
 * probably deprecated eventually for a "new" API that just coincidentally excludes music indexing.
 * Because go screw yourself for wanting to listen to music you own. Be a good consoomer and listen
 * to your AlgoPop StreamMix™ instead.
 *
 * I wish I was born in the neolithic.
 *
 * @author OxygenCobalt
 */
class MusicLoader {
    data class Library(
        val genres: List<Genre>,
        val artists: List<Artist>,
        val albums: List<Album>,
        val songs: List<Song>
    )

    @RequiresApi(Build.VERSION_CODES.R)
    fun load(context: Context): Library? {
        val songs = loadSongs(context)
        if (songs.isEmpty()) return null

        val albums = buildAlbums(songs)
        val artists = buildArtists(context, albums)
        val genres = readGenres(context, songs)

        return Library(
            genres,
            artists,
            albums,
            songs
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun loadSongs(context: Context): List<Song> {
        var songs = mutableListOf<Song>()
        val blacklistDatabase = ExcludedDatabase.getInstance(context)
        val paths = blacklistDatabase.readPaths()

        var selector = "${MediaStore.Audio.Media.IS_MUSIC}=1"
        val args = mutableListOf<String>()

        // DATA was deprecated on Android 10, but is set to be un-deprecated in Android 12L.
        // The only reason we'd want to change this is to add external partitions support, but
        // that's less efficent and there's no demand for that right now.
        for (path in paths) {
            selector += " AND ${MediaStore.Audio.Media.DATA} NOT LIKE ?"
            args += "$path%" // Append % so that the selector properly detects children
        }

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.DURATION,
            ),
            selector, args.toTypedArray(), null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val fileIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumArtistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
            val yearIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val fileName = cursor.getString(fileIndex)
                val title = cursor.getString(titleIndex) ?: fileName
                val album = cursor.getString(albumIndex)
                val albumId = cursor.getLong(albumIdIndex)

                // MediaStore does not have support for artists in the album field, so we have to
                // detect it on a song-by-song basis. This is another massive bottleneck in the music
                // loader since we have to do a massive query to get what we want, but theres not
                // a lot I can do that doesn't degrade UX.
                val artist = cursor.getStringOrNull(albumArtistIndex)
                    ?: cursor.getString(artistIndex)

                val year = cursor.getInt(yearIndex)
                val track = cursor.getInt(trackIndex)
                val duration = cursor.getLong(durationIndex)

                songs.add(
                    Song(
                        id, title, fileName, album, albumId, artist, year, track, duration
                    )
                )
            }
        }

        songs = songs.distinctBy {
            it.name to it.albumName to it.artistName to it.track to it.duration
        }.toMutableList()
        val settingsManager = SettingsManager.getInstance()
        if (!settingsManager.addAudios) {
            songs.removeAll { it.albumName.contains("Audio") }
            songs.removeAll { it.albumName.isDigitsOnly() }
        }

        return songs
    }

    private fun buildAlbums(songs: List<Song>): List<Album> {
        // Group up songs by their album ids and then link them with their albums
        val albums = mutableListOf<Album>()
        val songsByAlbum = songs.groupBy { it.albumId }

        songsByAlbum.forEach { entry ->
            // Use the song with the latest year as our metadata song.
            // This allows us to replicate the LAST_YEAR field, which is useful as it means that
            // weird years like "0" wont show up if there are alternatives.
            val song = requireNotNull(entry.value.maxByOrNull { it.year })

            albums.add(
                Album(
                    id = entry.key,
                    name = song.albumName,
                    artistName = song.artistName,
                    songs = entry.value,
                    year = song.year
                )
            )
        }

        albums.removeAll { it.songs.isEmpty() }

        return albums
    }

    private fun buildArtists(context: Context, albums: List<Album>): List<Artist> {
        val artists = mutableListOf<Artist>()
        val albumsByArtist = albums.groupBy { it.artistName }

        albumsByArtist.forEach { entry ->
            val resolvedName = if (entry.key == MediaStore.UNKNOWN_STRING) {
                context.getString(R.string.def_artist)
            } else {
                entry.key
            }

            // In most cases, MediaStore artist IDs are unreliable or omitted for speed.
            // Use the hashCode of the artist name as our ID and move on.
            artists.add(
                Artist(
                    id = entry.key.hashCode().toLong(),
                    name = entry.key,
                    resolvedName = resolvedName,
                    albums = entry.value
                )
            )
        }

        return artists
    }

    private fun readGenres(context: Context, songs: List<Song>): List<Genre> {
        val genres = mutableListOf<Genre>()

        // First, get a cursor for every genre in the android system
        val genreCursor = context.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Genres._ID,
                MediaStore.Audio.Genres.NAME
            ),
            null, null, null
        )

        // And then process those into Genre objects
        genreCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)

            while (cursor.moveToNext()) {
                // No non-broken genre would be missing a name.
                val id = cursor.getLong(idIndex)
                val name = cursor.getStringOrNull(nameIndex) ?: continue

                val genre = Genre(
                    id, name, name.getGenreNameCompat() ?: name
                )

                linkGenre(context, genre, songs)
                genres.add(genre)
            }
        }

        // Songs that don't have a genre will be thrown into an unknown genre.
        val unknownGenre = Genre(
            id = Long.MIN_VALUE,
            name = MediaStore.UNKNOWN_STRING,
            resolvedName = context.getString(R.string.def_genre)
        )

        songs.forEach { song ->
            if (song.genre == null) {
                unknownGenre.linkSong(song)
            }
        }

        if (unknownGenre.songs.isNotEmpty()) {
            genres.add(unknownGenre)
        }

        return genres
    }

    private fun linkGenre(context: Context, genre: Genre, songs: List<Song>) {
        val songCursor = context.contentResolver.query(
            MediaStore.Audio.Genres.Members.getContentUri("external", genre.id),
            arrayOf(MediaStore.Audio.Genres.Members._ID),
            null, null, null // Dont even bother blacklisting here as useless iters are less expensive than IO
        )

        songCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members._ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)

                songs.find { it.id == id }?.let { song ->
                    genre.linkSong(song)
                }
            }
        }
    }
}
