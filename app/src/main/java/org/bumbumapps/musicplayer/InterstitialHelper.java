package org.bumbumapps.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Single preloaded interstitial with a frequency cap. Call {@link #preload(Context)} once ads can be
 * requested and {@link #showIfReady(Activity, Runnable)} at natural break points; {@code next} always runs
 * exactly once (after the ad is dismissed, or immediately if no ad is shown).
 */
public final class InterstitialHelper {

    public static final long MIN_INTERVAL_MS = 60_000L;

    private static InterstitialAd interstitialAd;
    private static boolean loading;
    private static long lastShownAt = -MIN_INTERVAL_MS;

    private InterstitialHelper() {
    }

    public static void preload(Context context) {
        if (interstitialAd != null || loading || !AdsConsent.canRequestAds(context)) {
            return;
        }
        loading = true;
        InterstitialAd.load(context.getApplicationContext(), context.getString(R.string.admob_interstitial_id),
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loading = false;
                        interstitialAd = null;
                    }
                });
    }

    /** Pure frequency-cap rule, unit tested. */
    static boolean capAllows(long now, long lastShown) {
        return now - lastShown >= MIN_INTERVAL_MS;
    }

    public static void showIfReady(Activity activity, Runnable next) {
        final Runnable after = next != null ? next : () -> { };
        long now = SystemClock.elapsedRealtime();
        if (interstitialAd == null || activity == null || activity.isFinishing() || !capAllows(now, lastShownAt)) {
            if (activity != null) {
                preload(activity);
            }
            after.run();
            return;
        }
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preload(activity);
                after.run();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                preload(activity);
                after.run();
            }
        });
        lastShownAt = now;
        ad.show(activity);
    }
}
