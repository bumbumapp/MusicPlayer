package org.bumbumapps.musicplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialHelperTest {
    private static final long HOUR = 60 * 60_000L;
    private static final long T0 = 1_700_000_000_000L;

    @Test
    public void capIsOneHour() {
        assertEquals(HOUR, InterstitialHelper.MIN_INTERVAL_MS);
    }

    @Test
    public void noAdInTheFirstHourAfterInstall() {
        assertFalse(InterstitialHelper.capAllows(T0 + 59 * 60_000L, 0L, T0));
        assertTrue(InterstitialHelper.capAllows(T0 + HOUR, 0L, T0));
    }

    @Test
    public void atMostOneAdPerHour() {
        long shown = T0 + 2 * HOUR;
        assertFalse(InterstitialHelper.capAllows(shown + 10 * 60_000L, shown, T0));
        assertFalse(InterstitialHelper.capAllows(shown + 59 * 60_000L, shown, T0));
        assertTrue(InterstitialHelper.capAllows(shown + HOUR, shown, T0));
    }
}
