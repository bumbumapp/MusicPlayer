package org.bumbumapps.musicplayer

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import org.bumbumapps.musicplayer.Globals.TIMER_FINISHED


object AdsLoader {
    var mInterstitialAd: InterstitialAd? = null
    private var TAG = "TAG"
    fun displayInterstitial(context: Context) {
        mInterstitialAd=null
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(context,context.getString(R.string.ads_interstial), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(p0: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = p0
            }

        })

    }
    inline fun showAds(context: Context, crossinline unit:() -> Unit) {
        if (TIMER_FINISHED){
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(context as Activity)

                mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        TIMER_FINISHED=false
                        Timers.timer().start()
                        displayInterstitial(context)
                        unit()
                    }
                    override fun onAdShowedFullScreenContent() {
                        Log.d("TAG", "Ad showed fullscreen content.")
                        mInterstitialAd = null
                    }
                }

            } else {
                unit()
            }
        }else
        {
            unit()
        }

    }
}