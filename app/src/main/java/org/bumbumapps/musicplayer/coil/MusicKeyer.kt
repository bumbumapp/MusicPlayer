package org.bumbumapps.musicplayer.coil

import coil.key.Keyer
import coil.request.Options
import org.bumbumapps.musicplayer.music.Music
import org.bumbumapps.musicplayer.music.Song

/**
 * A basic keyer for music data.
 */
class MusicKeyer : Keyer<Music> {
    override fun key(data: Music, options: Options): String {
        return if (data is Song) {
            key(data.album, options)
        } else {
            "${data::class.simpleName}: ${data.id}"
        }
    }
}
