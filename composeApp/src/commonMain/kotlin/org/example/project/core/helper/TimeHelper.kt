package org.example.project.core.helper


fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return "%d:%02d".format(minutes, seconds)
}
