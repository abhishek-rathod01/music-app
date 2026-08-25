package com.abhishekrathod.musicapp.ui

/** `03:41`-style formatting. Returns "--:--" for an unknown duration, never a wrong-looking number. */
fun formatDuration(ms: Long?): String {
    if (ms == null || ms < 0) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
