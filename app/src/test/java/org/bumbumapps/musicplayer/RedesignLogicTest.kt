package org.bumbumapps.musicplayer

import org.bumbumapps.musicplayer.accent.ACCENT_COUNT
import org.bumbumapps.musicplayer.accent.ACCENT_DEFAULT
import org.bumbumapps.musicplayer.accent.Accent
import org.bumbumapps.musicplayer.detail.recycler.albumInfoLine
import org.bumbumapps.musicplayer.home.list.AlbumListFragment
import org.bumbumapps.musicplayer.playback.SleepTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Card #73: redesign + sleep timer + review prompt + ad pacing. */
class RedesignLogicTest {

    @Test
    fun auroraIsTheDefaultAccentAndKeepsOldIndices() {
        assertEquals(ACCENT_COUNT - 1, ACCENT_DEFAULT)
        assertEquals(R.style.Theme_Auxio_Aurora, Accent(ACCENT_DEFAULT).theme)
        assertEquals(R.style.Theme_Auxio_Black_Aurora, Accent(ACCENT_DEFAULT).blackTheme)
        // Saved index 5 (old default, Blue) still means Blue.
        assertEquals(R.style.Theme_Auxio_Blue, Accent(5).theme)
    }

    @Test
    fun darkIsTheDefaultThemeMode() {
        val compat = File("src/main/java/org/bumbumapps/musicplayer/settings/SettingsCompat.kt").readText()
        assertTrue(compat.contains("prefs.getInt(SettingsManager.KEY_THEME, AppCompatDelegate.MODE_NIGHT_YES)"))
    }

    @Test
    fun androidTwelvePlusNoLongerForcesDynamicColors() {
        for (dir in listOf("values-v31", "values-night-v31")) {
            val styles = File("src/main/res/$dir/styles_core.xml").readText()
            assertFalse(dir, styles.contains("m3_sys_color_dynamic_light_primary\""))
            assertFalse(dir, styles.contains("<item name=\"colorPrimary\">@color/m3_sys_color_dynamic"))
        }
    }

    @Test
    fun albumGridHasTwoColumnsOnPhones() {
        assertEquals(2, AlbumListFragment.albumGridColumns(1))
        assertEquals(4, AlbumListFragment.albumGridColumns(2))
    }

    @Test
    fun albumInfoSkipsMissingYear() {
        assertEquals("3 Songs • 1:15", albumInfoLine(0, "3 Songs", "1:15"))
        assertEquals("2024 • 3 Songs • 1:15", albumInfoLine(2024, "3 Songs", "1:15"))
    }

    @Test
    fun sleepTimerRemainingAndFormat() {
        assertEquals(0L, SleepTimer.remaining(0L, 5_000L))
        assertEquals(0L, SleepTimer.remaining(1_000L, 5_000L))
        assertEquals(60_000L, SleepTimer.remaining(65_000L, 5_000L))
        assertEquals("4:58", SleepTimer.format(297_500L))
        assertEquals("1:30:00", SleepTimer.format(90 * 60_000L))
        assertEquals("0:01", SleepTimer.format(1L))
    }

    @Test
    fun reviewPromptNeedsEngagementAndCooldown() {
        val day = 24L * 60 * 60 * 1000
        assertFalse(ReviewPrompt.shouldRequest(9, 5, 0, 100 * day))
        assertFalse(ReviewPrompt.shouldRequest(10, 1, 0, 100 * day))
        assertTrue(ReviewPrompt.shouldRequest(10, 2, 0, 100 * day))
        assertFalse(ReviewPrompt.shouldRequest(50, 9, 90 * day, 100 * day))
        assertTrue(ReviewPrompt.shouldRequest(50, 9, 60 * day, 100 * day))
    }

    @Test
    fun firstSongOfASessionNeverShowsAnAd() {
        assertFalse(AdsLoader.adAllowedForPlay(1))
        assertTrue(AdsLoader.adAllowedForPlay(2))
    }
}
