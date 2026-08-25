package com.abhishekrathod.musicapp.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abhishekrathod.musicapp.model.Track
import com.abhishekrathod.musicapp.model.TrackId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LibraryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val tracks =
        listOf(
            Track(TrackId("a"), "Song A", "Artist A", 100_000, null),
            Track(TrackId("b"), "Song B", "Artist B", 200_000, null),
        )

    @Test
    fun `renders every track's title and artist`() {
        composeTestRule.setContent { LibraryScreen(tracks = tracks, onTrackClick = {}) }

        composeTestRule.onNodeWithText("Song A").assertExists()
        composeTestRule.onNodeWithText("Artist A").assertExists()
        composeTestRule.onNodeWithText("Song B").assertExists()
        composeTestRule.onNodeWithText("Artist B").assertExists()
    }

    @Test
    fun `shows an empty-state message when the library has no tracks`() {
        composeTestRule.setContent { LibraryScreen(tracks = emptyList(), onTrackClick = {}) }

        composeTestRule.onNodeWithText("Your library is empty").assertExists()
    }

    @Test
    fun `clicking a row reports that row's index`() {
        var clickedIndex: Int? = null
        composeTestRule.setContent { LibraryScreen(tracks = tracks, onTrackClick = { clickedIndex = it }) }

        composeTestRule.onNodeWithText("Song B").performClick()

        assert(clickedIndex == 1) { "expected index 1, got $clickedIndex" }
    }
}
