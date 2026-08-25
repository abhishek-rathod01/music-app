package com.abhishekrathod.musicapp.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.stream.StreamResolver
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wraps a real `MediaController` connected to [PlaybackService]. Uses the
 * standard `SessionToken(Context, ComponentName)` constructor — the official
 * pattern for connecting from app UI code to a `MediaSessionService` on a
 * real device. (A Robolectric *test* of the equivalent connection needed a
 * different, session-token-based workaround for a shadow-specific
 * limitation — see `PlaybackServiceControllerTest`'s KDoc; that's a test
 * environment quirk, not a reason to change this production code, which
 * follows the documented, real-device-correct approach.)
 *
 * State strategy: [Player.Listener.onEvents] fires once per batch of player
 * changes rather than requiring a separate override per callback
 * (`onIsPlayingChanged`, `onMediaItemTransition`, ...); every firing just
 * rebuilds a full [PlaybackUiState] snapshot from the player's current
 * state. Simpler and harder to get subtly wrong than hand-tracking which
 * individual fields changed. A `positionMs` never updates on its own event
 * though — nothing pushes "the playhead moved" — so a small coroutine polls
 * it every 500ms while something is actually playing, cancelled whenever
 * playback isn't active or the controller disconnects.
 */
class MediaControllerPlaybackController(
    private val context: Context,
    private val streamResolver: StreamResolver,
) : PlaybackController {
    private val _state = MutableStateFlow(PlaybackUiState())
    override val state: StateFlow<PlaybackUiState> = _state

    private var controller: MediaController? = null
    private var scope: CoroutineScope? = null

    // TrackId.value -> Track, so queue/currentTrack in PlaybackUiState carry
    // full metadata rather than just what a MediaItem happens to expose.
    private val trackById = mutableMapOf<String, Track>()

    private val playerListener =
        object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                refreshState()
            }
        }

    override fun connect() {
        if (controller != null) return
        val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = controllerScope

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener(
            {
                val mediaController = future.get()
                controller = mediaController
                mediaController.addListener(playerListener)
                refreshState()
            },
            MoreExecutors.directExecutor(),
        )

        controllerScope.launch {
            while (isActive) {
                if (controller?.isPlaying == true) refreshState()
                delay(500)
            }
        }
    }

    override fun disconnect() {
        scope?.cancel()
        scope = null
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        _state.value = PlaybackUiState()
    }

    override suspend fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
        val mediaController = controller ?: return
        if (tracks.isEmpty()) return

        val mediaItems =
            tracks.map { track ->
                val resolved = streamResolver.resolve(track.id).getOrThrow()
                trackById[track.id.value] = track
                MediaItem.Builder()
                    .setMediaId(track.id.value)
                    .setUri(resolved.uri.value)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .build(),
                    )
                    .build()
            }

        mediaController.setMediaItems(mediaItems, startIndex.coerceIn(0, mediaItems.lastIndex), 0L)
        mediaController.prepare()
        mediaController.play()
        refreshState()
    }

    override fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    override fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    override fun skipToNext() {
        controller?.seekToNext()
    }

    override fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    override fun skipToQueueItem(index: Int) {
        controller?.seekTo(index, 0L)
    }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        controller?.moveMediaItem(fromIndex, toIndex)
    }

    override fun removeQueueItem(index: Int) {
        controller?.removeMediaItem(index)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        controller?.repeatMode =
            when (mode) {
                RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            }
    }

    override fun toggleShuffle() {
        val mediaController = controller ?: return
        mediaController.shuffleModeEnabled = !mediaController.shuffleModeEnabled
    }

    private fun refreshState() {
        val mediaController = controller ?: return
        val queue =
            (0 until mediaController.mediaItemCount).mapNotNull {
                trackById[mediaController.getMediaItemAt(it).mediaId]
            }
        val currentIndex = mediaController.currentMediaItemIndex.takeIf { mediaController.mediaItemCount > 0 }
        _state.value =
            PlaybackUiState(
                isConnected = true,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
                currentTrack = currentIndex?.let { trackById[mediaController.getMediaItemAt(it).mediaId] },
                positionMs = mediaController.currentPosition.coerceAtLeast(0L),
                durationMs = mediaController.duration.takeIf { it != C.TIME_UNSET },
                queue = queue,
                currentIndex = currentIndex,
                repeatMode =
                    when (mediaController.repeatMode) {
                        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                        else -> RepeatMode.OFF
                    },
                shuffled = mediaController.shuffleModeEnabled,
            )
    }
}
