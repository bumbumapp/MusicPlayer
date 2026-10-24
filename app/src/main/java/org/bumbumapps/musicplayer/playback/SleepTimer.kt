package org.bumbumapps.musicplayer.playback

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.bumbumapps.musicplayer.playback.state.PlaybackStateManager

/**
 * Pauses playback after a chosen number of minutes. Lives in the app process, so it keeps
 * running while the playback service plays in the background.
 */
object SleepTimer {
    /** Choices offered in the sleep timer dialog, in minutes. */
    val OPTIONS_MINUTES = intArrayOf(5, 15, 30, 45, 60, 90)

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var endsAt = 0L

    private val mActive = MutableLiveData(false)
    val active: LiveData<Boolean> get() = mActive

    private val fire = Runnable {
        endsAt = 0L
        mActive.value = false
        PlaybackStateManager.maybeGetInstance().setPlaying(false)
    }

    val isActive: Boolean get() = endsAt != 0L

    fun start(minutes: Int) {
        handler.removeCallbacks(fire)
        val delay = minutes * 60_000L
        endsAt = SystemClock.elapsedRealtime() + delay
        handler.postDelayed(fire, delay)
        mActive.value = true
    }

    fun cancel() {
        handler.removeCallbacks(fire)
        endsAt = 0L
        mActive.value = false
    }

    fun remainingMs(): Long = remaining(endsAt, SystemClock.elapsedRealtime())

    /** Milliseconds left until [endsAt], never negative; 0 when no timer is set. */
    fun remaining(endsAt: Long, now: Long): Long = if (endsAt == 0L) 0L else maxOf(0L, endsAt - now)

    /** "m:ss" below an hour, "h:mm:ss" above, rounded up to the next second. */
    fun format(ms: Long): String {
        val total = (ms + 999) / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
