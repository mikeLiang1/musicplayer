package org.example.project.core.helper


import kotlinx.serialization.json.*

/**
 * Recursively searches a JsonElement tree for an object containing [key],
 * returning the value at that key as a JsonObject.
 * Useful for InnerTube responses where nesting depth changes frequently.
 */
fun JsonElement.findObjectWithKey(key: String): JsonObject? {
    if (this is JsonObject) {
        if (this.containsKey(key)) return this[key]?.jsonObject
        for (value in this.values) {
            val found = value.findObjectWithKey(key)
            if (found != null) return found
        }
    } else if (this is JsonArray) {
        for (element in this) {
            val found = element.findObjectWithKey(key)
            if (found != null) return found
        }
    }
    return null
}

/**
 * Extracts plain text from YouTube's "runs" format:
 * { "runs": [ { "text": "Artist" }, { "text": " • " }, { "text": "Album" } ] }
 */
fun JsonObject?.getRunsText(): String {
    val runs = this?.get("runs")?.jsonArray ?: return "Unknown"
    return runs.joinToString("") {
        it.jsonObject["text"]?.jsonPrimitive?.content ?: ""
    }
}

fun JsonObject?.getRuns(): List<JsonObject>? {
    return this?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
        ?.get("text")?.jsonObject
        ?.get("runs")?.jsonArray
        ?.map { it.jsonObject }
}

fun List<JsonObject>.chunkedBySeparator(): List<List<JsonObject>> {
    val result = mutableListOf<MutableList<JsonObject>>()
    var current = mutableListOf<JsonObject>()

    for (run in this) {
        val text = run["text"]?.jsonPrimitive?.content ?: ""
        if (text == " • ") {
            result.add(current)
            current = mutableListOf()
        } else {
            current.add(run)
        }
    }
    result.add(current)
    return result
}

/**
 * Parses a YouTube duration string ("3:45", "1:02:30") into milliseconds.
 */
fun String.parseTimeToMillis(): Long {
    val parts = this.split(":").map { it.toLongOrNull() ?: 0L }
    return when (parts.size) {
        3 -> (parts[0] * 3600000) + (parts[1] * 60000) + (parts[2] * 1000) // HH:MM:SS
        2 -> (parts[0] * 60000) + (parts[1] * 1000)                         // MM:SS
        else -> 0L
    }
}


fun extractVideoId(url: String): String? = when {
    url.contains("v=") ->
        url.substringAfter("v=").substringBefore("&").takeIf { it.isNotBlank() }

    url.contains("youtu.be/") ->
        url.substringAfter("youtu.be/").substringBefore("?").takeIf { it.isNotBlank() }

    url.length == 11 -> url // raw video ID passed directly
    else -> null
}
