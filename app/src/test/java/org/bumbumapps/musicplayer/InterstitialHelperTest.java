package org.bumbumapps.musicplayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterstitialHelperTest {
    @Test
    public void frequencyCapBlocksShowsWithinSixtySeconds() {
        assertFalse(InterstitialHelper.capAllows(100_000L, 50_000L));
        assertTrue(InterstitialHelper.capAllows(110_000L, 50_000L));
    }

    @Test
    public void firstShowIsAllowed() {
        assertTrue(InterstitialHelper.capAllows(0L, -InterstitialHelper.MIN_INTERVAL_MS));
    }
}
