package com.abhishekrathod.musicapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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

        // Attempts 1 (onNodeWithContentDescription) and 2 (onNodeWithTag against the
        // default MERGED tree) both hit the identical silent failure in CI (see
        // FINDINGS.md): performClick() ran without throwing, but onItemClick was
        // never invoked. Per Android's own semantics-merging docs
        // (developer.android.com/develop/ui/compose/accessibility/merging-clearing),
        // a row with nested clickable descendants (this row's three IconButtons) is
        // exactly the documented case where matching against the default merged
        // tree can resolve ambiguously -- their own recommendation is
        // useUnmergedTree = true so the matcher targets this exact node directly,
        // bypassing however the merge collapses/exposes it for merged-tree queries.
        composeTestRule.onNodeWithTag("queue_item_2", useUnmergedTree = true).performClick()

        assert(clickedIndex == 2) { "expected index 2, got $clickedIndex" }
    }
}
