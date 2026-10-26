package org.bumbumapps.musicplayer

import android.Manifest
import android.os.Build
import android.os.SystemClock
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.bumbumapps.musicplayer.music.MusicStore
import org.bumbumapps.musicplayer.playback.PlaybackLayout
import org.bumbumapps.musicplayer.playback.PlaybackViewModel
import org.bumbumapps.musicplayer.ui.ActionMenu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Card #74: tapping a song must open Now Playing (it only played in the mini bar), and the
 * song menu is a bottom sheet with an icon row per action.
 */
@RunWith(AndroidJUnit4::class)
class SongTapOpensNowPlayingTest {
    @get:Rule
    val permissions: GrantPermissionRule = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun waitForMusic(): MusicStore? {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            MusicStore.maybeGetInstance()?.let { if (it.songs.isNotEmpty()) return it }
            SystemClock.sleep(200)
        }
        return null
    }

    @Test
    fun playingASongExpandsNowPlaying() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val store = waitForMusic()
            assumeTrue("Device has no music to play", store != null)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                val layout = activity.findViewById<PlaybackLayout>(R.id.playback_layout)
                layout.collapse()
                ViewModelProvider(activity)[PlaybackViewModel::class.java]
                    .playSong(store!!.songs.first())
            }

            var expanded = false
            val deadline = SystemClock.uptimeMillis() + 5_000
            while (!expanded && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(200)
                scenario.onActivity { activity ->
                    expanded = activity.findViewById<PlaybackLayout>(R.id.playback_layout).isExpanded
                }
            }

            assertTrue("Now Playing did not open after playing a song", expanded)
        }
    }

    @Test
    fun songMenuIsASheetWithIconRows() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val store = waitForMusic()
            assumeTrue("Device has no music", store != null)
            val song = store!!.songs.first()

            scenario.onActivity { activity ->
                val sheet = ActionMenu(activity, activity.window.decorView, song, ActionMenu.FLAG_NONE)
                sheet.show()

                val title = sheet.findViewById<TextView>(R.id.sheet_title)!!
                assertEquals(song.name, title.text.toString())

                val rows = sheet.findViewById<LinearLayout>(R.id.sheet_items)!!.children.toList()
                assertEquals(
                    listOf(
                        R.id.action_play_next, R.id.action_queue_add, R.id.action_go_artist,
                        R.id.action_go_album, R.id.action_share
                    ),
                    rows.map { it.id }
                )
                rows.forEach { row ->
                    assertTrue(row.findViewById<android.widget.ImageView>(R.id.sheet_action_icon).drawable != null)
                }
                sheet.dismiss()
            }
        }
    }
}
