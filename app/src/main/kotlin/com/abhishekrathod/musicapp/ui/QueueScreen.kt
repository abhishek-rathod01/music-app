package com.abhishekrathod.musicapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.abhishekrathod.musicapp.media.PlaybackUiState

/**
 * Up/down buttons rather than drag-and-drop, deliberately: a real drag
 * gesture over a `LazyColumn` is meaningfully more code and, with zero
 * ability to test touch gestures in this sandbox (see ENVIRONMENT.md — no
 * emulator, ever), meaningfully more risk to ship unverified. Buttons are
 * also more accessible by default (screen-reader-navigable, no gesture
 * coordination required) — a reasonable trade, not just a shortcut, though
 * upgrading to drag-and-drop once this can be tested on-device is a fair
 * ask for a future stage.
 */
@Composable
fun QueueScreen(
    state: PlaybackUiState,
    onItemClick: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.queue.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Queue is empty", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(state.queue.size) { index ->
            val track = state.queue[index]
            val isCurrent = index == state.currentIndex
            ListItem(
                headlineContent = { Text(track.title) },
                supportingContent = { Text(track.artist) },
                trailingContent = {
                    Row {
                        IconButton(onClick = { onMoveUp(index) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move ${track.title} up")
                        }
                        IconButton(onClick = { onMoveDown(index) }, enabled = index < state.queue.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move ${track.title} down")
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove ${track.title} from queue")
                        }
                    }
                },
                colors =
                    if (isCurrent) {
                        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    } else {
                        ListItemDefaults.colors()
                    },
                // testTag, not just contentDescription: CI (see FINDINGS.md) found
                // that a text- and then a contentDescription-based test query both
                // resolved performClick() to something other than this row's own
                // onClick action, in a row that also has three nested clickable
                // IconButtons -- testTag is the one semantics property Compose
                // testing documents as excluded from ancestor merging, so
                // onNodeWithTag targets this exact node with no merge-tree
                // ambiguity possible. Purely a testing hook; no effect on
                // production behavior or accessibility.
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("queue_item_$index")
                        .clickable { onItemClick(index) }
                        .semantics { contentDescription = "Play ${track.title} by ${track.artist}" },
            )
            HorizontalDivider()
        }
    }
}
