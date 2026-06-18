package org.example.project.core.parsers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.example.project.core.helper.findObjectWithKey
import org.example.project.core.helper.getRunsText
import org.example.project.core.helper.parseTimeToMillis
import org.example.project.core.model.Song
import org.example.project.core.model.SongPage

fun parseQueuePage(root: JsonObject): SongPage {
    val playlistPanel = root.findObjectWithKey("playlistPanelRenderer")
        ?: return SongPage(emptyList(), null)

    val contents = playlistPanel["contents"]?.jsonArray
        ?: return SongPage(emptyList(), null)

    val songs = contents.mapNotNull { parseQueueItem(it.jsonObject) }

    val token = playlistPanel["continuations"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("nextRadioContinuationData")
        ?.jsonObject
        ?.get("continuation")
        ?.jsonPrimitive
        ?.content

    return SongPage(songs, token)
}

fun parseQueueItem(item: JsonObject): Song? {
    val renderer = item["playlistPanelVideoRenderer"]?.jsonObject
        ?: return null

    val videoId = renderer["videoId"]?.jsonPrimitive?.content
        ?: return null

    return Song(
        uniqueId = videoId,
        url = "https://www.youtube.com/watch?v=$videoId",
        title = renderer["title"]?.jsonObject.getRunsText(),
        artist = renderer["longBylineText"]?.jsonObject
            .getRunsText()
            .split(" • ")
            .first(),
        thumbnailUrl = renderer["thumbnail"]
            ?.jsonObject
            ?.get("thumbnails")
            ?.jsonArray
            ?.lastOrNull()
            ?.jsonObject
            ?.get("url")
            ?.jsonPrimitive
            ?.content,
        duration = renderer["lengthText"]
            ?.jsonObject
            ?.get("runs")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            ?.parseTimeToMillis() ?: 0L
    )
}
