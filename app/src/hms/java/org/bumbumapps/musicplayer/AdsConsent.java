package org.bumbumapps.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Huawei AppGallery build: same API as the Play build's UMP flow, but initialises Huawei Ads (Petal).
 * Only non-personalized ads are requested, so no consent form / privacy options are needed.
 */
public final class AdsConsent {

    private static final String TAG = "AdsConsent";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile boolean ready;

    private AdsConsent() {
    }

    private static void ensureStarted(Context context) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        try {
            HwAds.init(context.getApplicationContext());
            RequestOptions options = HwAds.getRequestOptions().toBuilder()
                    .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                    .build();
            HwAds.setRequestOptions(options);
            ready = true;
        } catch (RuntimeException e) {
            Log.w(TAG, "Huawei Ads init failed", e);
        }
    }

    /** Initialises Huawei Ads (non-personalized), then runs {@code onReady}. */
    public static void gather(Activity activity, Runnable onReady) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        ensureStarted(activity);
        if (ready && onReady != null) {
            onReady.run();
        }
    }

    public static boolean canRequestAds(Context context) {
        ensureStarted(context);
        return ready;
    }

    public static boolean isPrivacyOptionsRequired(Context context) {
        return false;
    }

    public static void showPrivacyOptions(Activity activity) {
    }
}
