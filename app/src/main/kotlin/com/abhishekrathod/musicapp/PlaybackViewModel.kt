package com.abhishekrathod.musicapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.media.MediaControllerPlaybackController
import com.abhishekrathod.musicapp.media.PlaybackController
import com.abhishekrathod.musicapp.media.PlaybackUiState
import com.abhishekrathod.musicapp.media.SampleTrack
import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.stream.FakeStreamResolver
import com.abhishekrathod.musicapp.stream.StreamUri
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The one place `:app` constructs a [PlaybackController] and hands it to
 * every screen — library, player, and queue all share this single instance
 * (scoped to `MainActivity` via Compose's default `viewModel()`), so they're
 * all observing and commanding the same live playback state. No screen
 * constructs its own controller or touches Media3 types directly, per the
 * Phase C constraint.
 *
 * Hilt is on CLAUDE.md's approved stack but deliberately not wired in
 * tonight: this is the one and only place a [PlaybackController] gets
 * constructed, so a constructor call here is exactly as testable as a Hilt
 * binding would be, without adding a whole new, unverified annotation-
 * processing surface on top of everything else built tonight. Worth
 * revisiting once there's more than one place needing this wiring.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    private val controller: PlaybackController =
        MediaControllerPlaybackController(
            context = application,
            streamResolver = FakeStreamResolver(localUri = StreamUri(SampleTrack.uri(application.packageName))),
        )

    val state: StateFlow<PlaybackUiState> = controller.state

    init {
        controller.connect()
    }

    fun playFromLibrary(tracks: List<Track>, startIndex: Int) {
        viewModelScope.launch { controller.setQueueAndPlay(tracks, startIndex) }
    }

    fun togglePlayPause() = controller.togglePlayPause()

    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    fun skipToNext() = controller.skipToNext()

    fun skipToPrevious() = controller.skipToPrevious()

    fun skipToQueueItem(index: Int) = controller.skipToQueueItem(index)

    fun moveQueueItem(fromIndex: Int, toIndex: Int) = controller.moveQueueItem(fromIndex, toIndex)

    fun removeQueueItem(index: Int) = controller.removeQueueItem(index)

    fun setRepeatMode(mode: RepeatMode) = controller.setRepeatMode(mode)

    fun toggleShuffle() = controller.toggleShuffle()

    override fun onCleared() {
        controller.disconnect()
        super.onCleared()
    }
}
