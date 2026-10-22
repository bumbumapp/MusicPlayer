package org.bumbumapps.musicplayer

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Regression: targeting Android 14 without FOREGROUND_SERVICE_MEDIA_PLAYBACK makes
 * startForeground() throw SecurityException; without POST_NOTIFICATIONS the player
 * notification is invisible on Android 13+.
 */
class ManifestPermissionsTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun declaresMediaPlaybackForegroundServicePermission() {
        assertTrue(manifest.contains("android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"))
        assertTrue(manifest.contains("android:foregroundServiceType=\"mediaPlayback\""))
    }

    @Test
    fun declaresNotificationAndAudioPermissions() {
        assertTrue(manifest.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(manifest.contains("android.permission.READ_MEDIA_AUDIO"))
    }
}
