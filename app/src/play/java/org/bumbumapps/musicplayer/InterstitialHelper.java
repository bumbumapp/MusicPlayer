package org.bumbumapps.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Single preloaded interstitial with a strict frequency cap: at most one per hour, and none during the
 * first hour after install (banners are the main ad; frequent full-screen ads make listeners uninstall).
 * The cap is stored in SharedPreferences so it survives app restarts. Call {@link #preload(Context)} once ads can be
 * requested and {@link #showIfReady(Activity, Runnable)} at natural break points; {@code next} always runs
 * exactly once (after the ad is dismissed, or immediately if no ad is shown).
 */
public final class InterstitialHelper {

    public static final long MIN_INTERVAL_MS = 60 * 60_000L;

    private static final String PREFS = "interstitial_cap";
    private static final String KEY_LAST = "last_shown_ms";
    private static final String KEY_FIRST_SEEN = "first_seen_ms";

    private static InterstitialAd interstitialAd;
    private static boolean loading;

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

    /**
     * Pure frequency-cap rule, unit tested: an hour must have passed since the last show, and since the
     * app was first seen (so a new player never gets a full-screen ad in the first hour).
     */
    static boolean capAllows(long now, long lastShown, long firstSeen) {
        return now - Math.max(lastShown, firstSeen) >= MIN_INTERVAL_MS;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static long firstSeen(SharedPreferences p, long now) {
        long first = p.getLong(KEY_FIRST_SEEN, 0L);
        if (first == 0L) {
            first = now;
            p.edit().putLong(KEY_FIRST_SEEN, first).apply();
        }
        return first;
    }

    public static void showIfReady(Activity activity, Runnable next) {
        final Runnable after = next != null ? next : () -> { };
        long now = System.currentTimeMillis();
        SharedPreferences p = activity != null ? prefs(activity) : null;
        if (interstitialAd == null || p == null || activity.isFinishing()
                || !capAllows(now, p.getLong(KEY_LAST, 0L), firstSeen(p, now))) {
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
        p.edit().putLong(KEY_LAST, now).apply();
        ad.show(activity);
    }
}
