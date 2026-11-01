package org.bumbumapps.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.InterstitialAd;

/**
 * Huawei AppGallery build: same API and frequency cap as the Play build (at most one per hour, none in
 * the first hour after install), served by Huawei Ads instead of AdMob. {@code next} always runs exactly once.
 */
public final class InterstitialHelper {

    public static final long MIN_INTERVAL_MS = 60 * 60_000L;

    private static final String PREFS = "interstitial_cap";
    private static final String KEY_LAST = "last_shown_ms";
    private static final String KEY_FIRST_SEEN = "first_seen_ms";

    private static InterstitialAd interstitialAd;

    private InterstitialHelper() {
    }

    public static void preload(Context context) {
        if (interstitialAd != null || !AdsConsent.canRequestAds(context)) {
            return;
        }
        final InterstitialAd ad = new InterstitialAd(context.getApplicationContext());
        ad.setAdId(context.getString(BuildConfig.DEBUG ? R.string.hms_test_interstitial_id : R.string.hms_interstitial_id));
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                if (interstitialAd == ad) {
                    interstitialAd = null;
                }
            }
        });
        interstitialAd = ad;
        ad.loadAd(new AdParam.Builder().build());
    }

    /** Pure frequency-cap rule, unit tested (same as the Play build). */
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
        InterstitialAd ad = interstitialAd;
        if (ad == null || !ad.isLoaded() || p == null || activity.isFinishing()
                || !capAllows(now, p.getLong(KEY_LAST, 0L), firstSeen(p, now))) {
            if (activity != null && ad == null) {
                preload(activity);
            }
            after.run();
            return;
        }
        interstitialAd = null;
        // Huawei only reports load failures via onAdFailed: if show() never opens the ad, do not wait forever.
        final boolean[] opened = {false};
        final boolean[] done = {false};
        final Runnable finish = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            preload(activity);
            after.run();
        };
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdOpened() {
                opened[0] = true;
            }

            @Override
            public void onAdClosed() {
                finish.run();
            }

            @Override
            public void onAdFailed(int code) {
                finish.run();
            }
        });
        p.edit().putLong(KEY_LAST, now).apply();
        ad.show(activity);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!opened[0]) {
                finish.run();
            }
        }, 3000L);
    }
}
