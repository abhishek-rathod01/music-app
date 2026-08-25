package com.abhishekrathod.musicapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhishekrathod.musicapp.data.queue.RepeatMode
import com.abhishekrathod.musicapp.media.PlaybackUiState
import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.model.TrackId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PlayerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val track = Track(TrackId("a"), "Song A", "Artist A", 100_000, null)

    @Test
    fun `shows a placeholder when nothing is playing`() {
        composeTestRule.setContent {
            PlayerScreen(
                state = PlaybackUiState(),
                onPlayPause = {}, onSeek = {}, onNext = {}, onPrevious = {}, onToggleShuffle = {}, onCycleRepeat = {},
            )
        }

        composeTestRule.onNodeWithText("Nothing playing").assertExists()
    }

    @Test
    fun `shows the current track's title and artist`() {
        composeTestRule.setContent {
            PlayerScreen(
                state = PlaybackUiState(currentTrack = track, isPlaying = true),
                onPlayPause = {}, onSeek = {}, onNext = {}, onPrevious = {}, onToggleShuffle = {}, onCycleRepeat = {},
            )
        }

        composeTestRule.onNodeWithText("Song A").assertExists()
        composeTestRule.onNodeWithText("Artist A").assertExists()
    }

    @Test
    fun `play-pause control reflects playing state and reports a click`() {
        var toggled = false
        composeTestRule.setContent {
            PlayerScreen(
                state = PlaybackUiState(currentTrack = track, isPlaying = true),
                onPlayPause = { toggled = true }, onSeek = {}, onNext = {}, onPrevious = {},
                onToggleShuffle = {}, onCycleRepeat = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Pause").performClick()

        assert(toggled) { "expected onPlayPause to have been called" }
    }

    @Test
    fun `next and previous controls report clicks`() {
        var nextClicked = false
        var previousClicked = false
        composeTestRule.setContent {
            PlayerScreen(
                state = PlaybackUiState(currentTrack = track),
                onPlayPause = {}, onSeek = {},
                onNext = { nextClicked = true }, onPrevious = { previousClicked = true },
                onToggleShuffle = {}, onCycleRepeat = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Next").performClick()
        composeTestRule.onNodeWithContentDescription("Previous").performClick()

        assert(nextClicked && previousClicked)
    }

    @Test
    fun `repeat control cycles OFF to ALL`() {
        var reported: RepeatMode? = null
        composeTestRule.setContent {
            PlayerScreen(
                state = PlaybackUiState(currentTrack = track, repeatMode = RepeatMode.OFF),
                onPlayPause = {}, onSeek = {}, onNext = {}, onPrevious = {}, onToggleShuffle = {},
                onCycleRepeat = { reported = it },
            )
        }

        composeTestRule.onNodeWithContentDescription("Repeat off").performClick()

        assert(reported == RepeatMode.ALL) { "expected ALL, got $reported" }
    }
}
