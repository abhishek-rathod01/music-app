package com.abhishekrathod.musicapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.media.PlaybackUiState

/**
 * Art, title, scrubber, transport controls — one screen, one clear focal
 * point, per the brief. No artwork loading yet (Coil isn't wired in and
 * FakeLibrary has no artwork URLs — see FakeLibrary.kt); a placeholder icon
 * fills that slot honestly instead of leaving a blank gap or faking a real
 * image.
 */
@Composable
fun PlayerScreen(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: (RepeatMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.padding(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(modifier = Modifier.padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = state.currentTrack?.title ?: "Nothing playing",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = state.currentTrack?.artist.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Scrubber(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        )

        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val shuffleTint =
                if (state.shuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            IconButton(onClick = onToggleShuffle) {
                Icon(
                    Icons.Filled.Shuffle,
                    contentDescription = if (state.shuffled) "Shuffle on" else "Shuffle off",
                    tint = shuffleTint,
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            val playPauseDescription = if (state.isPlaying) "Pause" else "Play"
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.semantics { contentDescription = playPauseDescription },
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp),
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
            val (repeatIcon, repeatDescription) = repeatIconFor(state.repeatMode)
            val repeatTint =
                if (state.repeatMode == RepeatMode.OFF) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
            IconButton(onClick = { onCycleRepeat(nextRepeatMode(state.repeatMode)) }) {
                Icon(repeatIcon, contentDescription = repeatDescription, tint = repeatTint)
            }
        }
    }
}

@Composable
private fun Scrubber(
    positionMs: Long,
    durationMs: Long?,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // While the user is actively dragging, show their drag position instead
    // of the live (possibly still advancing) playback position — otherwise
    // the thumb would fight the user's own gesture.
    var dragPositionMs by remember { mutableStateOf<Long?>(null) }
    val duration = durationMs?.coerceAtLeast(1L) ?: 1L
    val displayedPositionMs = dragPositionMs ?: positionMs

    Column(modifier = modifier) {
        Slider(
            value = (displayedPositionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
            onValueChange = { fraction -> dragPositionMs = (fraction * duration).toLong() },
            onValueChangeFinished = {
                dragPositionMs?.let(onSeek)
                dragPositionMs = null
            },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(displayedPositionMs), style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun repeatIconFor(mode: RepeatMode): Pair<ImageVector, String> =
    when (mode) {
        RepeatMode.OFF -> Icons.Filled.Repeat to "Repeat off"
        RepeatMode.ALL -> Icons.Filled.Repeat to "Repeat all"
        RepeatMode.ONE -> Icons.Filled.RepeatOne to "Repeat one"
    }

private fun nextRepeatMode(current: RepeatMode): RepeatMode =
    when (current) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
    }
