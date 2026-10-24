package org.bumbumapps.musicplayer.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.music.toURI

/** The text that goes along with a shared song file. */
fun shareText(song: Song): String = "${song.name} — ${song.album.artist.resolvedName}"

/** Share the audio file of [song] through the system share sheet. */
fun Context.shareSong(song: Song) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("audio/*")
        .putExtra(Intent.EXTRA_STREAM, song.id.toURI())
        .putExtra(Intent.EXTRA_TEXT, shareText(song))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        startActivity(Intent.createChooser(send, getString(R.string.lbl_share_song)))
    } catch (e: ActivityNotFoundException) {
        showToast(R.string.err_share_song)
    }
}
