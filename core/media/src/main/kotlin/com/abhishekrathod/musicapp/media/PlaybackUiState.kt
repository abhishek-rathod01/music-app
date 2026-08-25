package com.abhishekrathod.musicapp.media

import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.model.Track

/**
 * Everything a screen could need to render playback — a snapshot, not a
 * stream of individual events. Screens observe one `StateFlow<PlaybackUiState>`
 * (see [PlaybackController.state]) rather than wiring up separate listeners
 * for "is it playing", "what's the queue", "what's the position" themselves;
 * that's exactly what "a thin interface wrapping MediaController" buys —
 * a screen that reaches into a raw `MediaController` would need to know
 * about `Player.Listener` callbacks at all.
 */
data class PlaybackUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val durationMs: Long? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffled: Boolean = false,
)
