package com.abhishekrathod.musicapp

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.abhishekrathod.musicapp.media.PlaybackService
import com.abhishekrathod.musicapp.media.SampleTrack
import com.google.common.util.concurrent.MoreExecutors

// Phase B: proves the whole pipeline — PlaybackService, its MediaSession,
// and a MediaController built here in the UI layer — works end to end,
// playing the one bundled sample track. This screen is deliberately
// throwaway: Phase C replaces it with the real player/library/queue screens,
// built against a proper thin controller-wrapping interface (see
// ARCHITECTURE.md and PLAN.md Stage 3). For now, this is the minimum needed
// to prove the pipeline on a real device.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PlaybackProofOfConceptScreen()
                }
            }
        }
    }
}

@Composable
private fun PlaybackProofOfConceptScreen() {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    // Building a MediaController is asynchronous — it has to bind to
    // PlaybackService across a process boundary (even though today it's the
    // same process, Media3 always treats it this way) — so this connects on
    // first composition and, critically, releases the controller in
    // onDispose. Never holding onto a MediaController past the screen's
    // lifecycle is what keeps this UI a *client* of playback rather than an
    // owner of it, per ARCHITECTURE.md constraint 2.
    DisposableEffect(Unit) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        val playerListener =
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
            }
        controllerFuture.addListener(
            {
                val mediaController = controllerFuture.get()
                mediaController.addListener(playerListener)
                controller = mediaController
            },
            MoreExecutors.directExecutor(),
        )

        onDispose {
            controller?.removeListener(playerListener)
            controller?.release()
            controller = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Phase B: playback pipeline check",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                val mediaController = controller ?: return@Button
                if (mediaController.mediaItemCount == 0) {
                    mediaController.setMediaItem(MediaItem.fromUri(SampleTrack.uri(context.packageName)))
                    mediaController.prepare()
                }
                if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
            },
        ) {
            Text(if (isPlaying) "Pause" else "Play sample track")
        }
    }
}
