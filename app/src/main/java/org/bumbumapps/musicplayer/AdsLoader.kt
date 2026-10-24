package org.bumbumapps.musicplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Interstitials go through [InterstitialHelper]: preloaded once, frequency capped, consent gated. */
object AdsLoader {

    fun displayInterstitial(context: Context) {
        InterstitialHelper.preload(context)
    }

    /** Song taps in this process; the first one never shows an ad. */
    private var plays = 0

    /** The first play of a session goes straight to the music, so opening the app never hits an ad. */
    fun adAllowedForPlay(playNumber: Int): Boolean = playNumber > 1

    fun showAds(context: Context, unit: () -> Unit) {
        val activity = context.findActivity()
        plays++
        if (activity == null || !adAllowedForPlay(plays)) {
            unit()
        } else {
            InterstitialHelper.showIfReady(activity) { unit() }
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
