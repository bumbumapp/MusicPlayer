package org.bumbumapps.musicplayer

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play Store rating with the In-App Review API once someone clearly enjoys the app:
 * from the [MIN_SONGS]th song played, not in the first launch, at most once per [COOLDOWN_MS]
 * locally (Google applies its own quota on top). Never bound to a button.
 */
object ReviewPrompt {
    const val MIN_SONGS = 10
    const val MIN_LAUNCHES = 2
    const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

    private const val TAG = "ReviewPrompt"
    private const val PREFS = "review_prompt"
    private const val KEY_LAUNCHES = "launches"
    private const val KEY_SONGS = "songs"
    private const val KEY_LAST_REQUEST = "last_request"

    /** Pure decision logic (unit tested). */
    fun shouldRequest(songs: Int, launches: Int, lastRequestMs: Long, nowMs: Long): Boolean {
        if (songs < MIN_SONGS || launches < MIN_LAUNCHES) return false
        return lastRequestMs <= 0 || nowMs - lastRequestMs >= COOLDOWN_MS
    }

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Call once per process start. */
    fun onAppLaunch(c: Context) {
        val p = prefs(c)
        p.edit { putInt(KEY_LAUNCHES, p.getInt(KEY_LAUNCHES, 0) + 1) }
    }

    /** Records a song that started playing; true if the review flow should start now. */
    fun recordSongPlayed(c: Context): Boolean {
        val p = prefs(c)
        val songs = p.getInt(KEY_SONGS, 0) + 1
        val now = System.currentTimeMillis()
        val ask = shouldRequest(songs, p.getInt(KEY_LAUNCHES, 0), p.getLong(KEY_LAST_REQUEST, 0L), now)
        p.edit {
            putInt(KEY_SONGS, songs)
            if (ask) putLong(KEY_LAST_REQUEST, now)
        }
        return ask
    }

    /** Starts Play's in-app review flow; Google decides whether the sheet is actually shown. */
    fun launch(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (!task.isSuccessful || activity.isFinishing || activity.isDestroyed) {
                    return@addOnCompleteListener
                }
                manager.launchReviewFlow(activity, task.result)
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "In-app review unavailable", e)
        }
    }
}
