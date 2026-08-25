package com.abhishekrathod.musicapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhishekrathod.musicapp.media.PlaybackUiState
import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.model.TrackId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class QueueScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val queue =
        listOf(
            Track(TrackId("a"), "Song A", "Artist A", 100_000, null),
            Track(TrackId("b"), "Song B", "Artist B", 200_000, null),
            Track(TrackId("c"), "Song C", "Artist C", 150_000, null),
        )

    @Test
    fun `shows an empty-state message when the queue is empty`() {
        composeTestRule.setContent {
            QueueScreen(state = PlaybackUiState(), onItemClick = {}, onMoveUp = {}, onMoveDown = {}, onRemove = {})
        }

        composeTestRule.onNodeWithText("Queue is empty").assertExists()
    }

    @Test
    fun `renders every queued track`() {
        composeTestRule.setContent {
            QueueScreen(
                state = PlaybackUiState(queue = queue, currentIndex = 0),
                onItemClick = {}, onMoveUp = {}, onMoveDown = {}, onRemove = {},
            )
        }

        composeTestRule.onNodeWithText("Song A").assertExists()
        composeTestRule.onNodeWithText("Song B").assertExists()
        composeTestRule.onNodeWithText("Song C").assertExists()
    }

    @Test
    fun `moving the middle item up reports its index`() {
        var movedUpIndex: Int? = null
        composeTestRule.setContent {
            QueueScreen(
                state = PlaybackUiState(queue = queue, currentIndex = 0),
                onItemClick = {}, onMoveUp = { movedUpIndex = it }, onMoveDown = {}, onRemove = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("Move Song B up").performClick()

        assert(movedUpIndex == 1) { "expected index 1, got $movedUpIndex" }
    }

    @Test
    fun `removing an item reports its index`() {
        var removedIndex: Int? = null
        composeTestRule.setContent {
            QueueScreen(
                state = PlaybackUiState(queue = queue, currentIndex = 0),
                onItemClick = {}, onMoveUp = {}, onMoveDown = {}, onRemove = { removedIndex = it },
            )
        }

        composeTestRule.onNodeWithContentDescription("Remove Song C from queue").performClick()

        assert(removedIndex == 2) { "expected index 2, got $removedIndex" }
    }

    @Test
    fun `clicking a queued track reports its index`() {
        var clickedIndex: Int? = null
        composeTestRule.setContent {
            QueueScreen(
                state = PlaybackUiState(queue = queue, currentIndex = 0),
                onItemClick = { clickedIndex = it }, onMoveUp = {}, onMoveDown = {}, onRemove = {},
            )
        }

        // Not onNodeWithText("Song C") -- CI run #16 found that ambiguous here (see
        // FINDINGS.md): the row's own clickable node merges its headline/supporting
        // Text into itself for accessibility, EXCEPT this row also contains three
        // independently-clickable IconButtons (move up/down, remove), each of which
        // is its own merge boundary. onNodeWithText matched something in that tree,
        // but performClick() on it never reached the row's own onClick action, so
        // onItemClick was never invoked. The row's contentDescription is set
        // directly on the same clickable node as the onClick action itself (see
        // QueueScreen.kt), so it names that exact node unambiguously -- the same
        // pattern already used successfully by this file's move/remove tests above.
        composeTestRule.onNodeWithContentDescription("Play Song C by Artist C").performClick()

        assert(clickedIndex == 2) { "expected index 2, got $clickedIndex" }
    }
}
