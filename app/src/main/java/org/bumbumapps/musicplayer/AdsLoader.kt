package org.bumbumapps.musicplayer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Interstitials go through [InterstitialHelper]: preloaded once, frequency capped, consent gated. */
object AdsLoader {

    fun displayInterstitial(context: Context) {
        InterstitialHelper.preload(context)
    }

    fun showAds(context: Context, unit: () -> Unit) {
        val activity = context.findActivity()
        if (activity == null) {
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
