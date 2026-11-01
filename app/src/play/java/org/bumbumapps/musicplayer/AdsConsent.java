package org.bumbumapps.musicplayer;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.ads.MobileAds;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Google UMP consent flow. Ads may only be initialised / requested after
 * {@link #canRequestAds(Context)} returns true.
 */
public final class AdsConsent {

    private static final AtomicBoolean MOBILE_ADS_STARTED = new AtomicBoolean(false);

    private AdsConsent() {
    }

    /** Requests a consent info update, shows the form if required, then runs {@code onReady} once ads can be requested. */
    public static void gather(Activity activity, Runnable onReady) {
        final ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        info.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (info.canRequestAds()) {
                        startAds(activity, onReady);
                    }
                }),
                requestError -> {
                    // Offline or misconfigured: fall back to whatever consent was stored previously.
                    if (info.canRequestAds()) {
                        startAds(activity, onReady);
                    }
                });
        // Consent obtained in a previous session: start right away, don't wait for the network.
        if (info.canRequestAds()) {
            startAds(activity, onReady);
        }
    }

    public static boolean canRequestAds(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    public static boolean isPrivacyOptionsRequired(Context context) {
        return UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
        });
    }

    private static void startAds(Activity activity, Runnable onReady) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (MOBILE_ADS_STARTED.compareAndSet(false, true)) {
            final Context app = activity.getApplicationContext();
            new Thread(() -> MobileAds.initialize(app, status -> {
            })).start();
        }
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed() && onReady != null) {
                onReady.run();
            }
        });
    }
}
