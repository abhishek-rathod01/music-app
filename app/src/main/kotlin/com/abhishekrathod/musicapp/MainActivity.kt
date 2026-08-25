package com.abhishekrathod.musicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhishekrathod.musicapp.ui.LibraryScreen
import com.abhishekrathod.musicapp.ui.PlayerScreen
import com.abhishekrathod.musicapp.ui.QueueScreen

/**
 * Single-activity host (per CLAUDE.md) for the three Phase C screens, tied
 * together by one shared [PlaybackViewModel] so they're always observing
 * and commanding the same live playback state. Dark theme first, per the
 * brief — `MaterialTheme` here doesn't force `darkColorScheme()`
 * explicitly; it follows the system default, which on a fresh install of
 * this personal, single-device app is the meaningful "first" theme to
 * design against.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    MusicAppHost()
                }
            }
        }
    }
}

private enum class Destination(val label: String) {
    LIBRARY("Library"),
    PLAYER("Player"),
    QUEUE("Queue"),
}

@Composable
private fun MusicAppHost(viewModel: PlaybackViewModel = viewModel()) {
    var destination by remember { mutableStateOf(Destination.LIBRARY) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = destination == Destination.LIBRARY,
                    onClick = { destination = Destination.LIBRARY },
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text(Destination.LIBRARY.label) },
                )
                NavigationBarItem(
                    selected = destination == Destination.PLAYER,
                    onClick = { destination = Destination.PLAYER },
                    icon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                    label = { Text(Destination.PLAYER.label) },
                )
                NavigationBarItem(
                    selected = destination == Destination.QUEUE,
                    onClick = { destination = Destination.QUEUE },
                    icon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                    label = { Text(Destination.QUEUE.label) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (destination) {
                Destination.LIBRARY ->
                    LibraryScreen(
                        tracks = FakeLibrary.tracks,
                        onTrackClick = { index ->
                            viewModel.playFromLibrary(FakeLibrary.tracks, index)
                            destination = Destination.PLAYER
                        },
                    )

                Destination.PLAYER ->
                    PlayerScreen(
                        state = state,
                        onPlayPause = viewModel::togglePlayPause,
                        onSeek = viewModel::seekTo,
                        onNext = viewModel::skipToNext,
                        onPrevious = viewModel::skipToPrevious,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onCycleRepeat = viewModel::setRepeatMode,
                    )

                Destination.QUEUE ->
                    QueueScreen(
                        state = state,
                        onItemClick = viewModel::skipToQueueItem,
                        onMoveUp = { index -> viewModel.moveQueueItem(index, index - 1) },
                        onMoveDown = { index -> viewModel.moveQueueItem(index, index + 1) },
                        onRemove = viewModel::removeQueueItem,
                    )
            }
        }
    }
}
