package com.abhishekrathod.musicapp.model

@JvmInline
value class PlaylistId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlaylistId must not be blank" }
    }
}

/**
 * A named, ordered list of tracks. Deliberately holds [TrackId]s rather than
 * full [Track] objects — a playlist is a statement about order and
 * membership, not a cache of metadata. Metadata lives in exactly one place
 * (Room, per ARCHITECTURE.md); duplicating it here would let a playlist's
 * copy of a title drift from the library's.
 */
data class Playlist(
    val id: PlaylistId,
    val name: String,
    val trackIds: List<TrackId>,
) {
    init {
        require(name.isNotBlank()) { "Playlist name must not be blank" }
    }
}
