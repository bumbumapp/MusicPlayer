package org.bumbumapps.musicplayer

import org.bumbumapps.musicplayer.music.Song
import org.bumbumapps.musicplayer.playback.nextUpOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression (card #73): after a restored playback state the queue screen listed the whole queue,
 * including the playing and already played songs, and drag/swipe edited the wrong songs.
 */
class QueueNextUpTest {
    private fun song(id: Long) = Song(id, "s$id", "s$id.mp3", "a", 1, "x", 0, id.toInt(), 1000)

    @Test
    fun nextUpStartsAfterThePlayingSong() {
        val queue = (1L..5L).map(::song)
        assertEquals(listOf(4L, 5L), nextUpOf(queue, 2).map { it.id })
        assertEquals(emptyList<Long>(), nextUpOf(queue, 4).map { it.id })
        assertEquals((2L..5L).toList(), nextUpOf(queue, 0).map { it.id })
    }
}
