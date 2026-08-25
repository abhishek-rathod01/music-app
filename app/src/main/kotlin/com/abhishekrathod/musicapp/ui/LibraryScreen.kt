package com.abhishekrathod.musicapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.abhishekrathod.musicapp.model.Track

/**
 * Flat browse list — one obvious action per row (tap to play), per the
 * brief's "clean and uncluttered" constraint. No search, no sort, no
 * filters yet: those are real features for when there's a real library
 * (Stage 3/4), not chrome to add now around six hardcoded rows.
 */
@Composable
fun LibraryScreen(
    tracks: List<Track>,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tracks.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your library is empty", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(tracks.size) { index ->
            val track = tracks[index]
            ListItem(
                headlineContent = { Text(track.title) },
                supportingContent = { Text(track.artist) },
                trailingContent = { Text(formatDuration(track.durationMs)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(index) }
                        .semantics { contentDescription = "Play ${track.title} by ${track.artist}" },
            )
            HorizontalDivider()
        }
    }
}
