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

/**
 * Parses a YouTube duration string ("3:45", "1:02:30") into milliseconds.
 */
fun String.parseTimeToMillis(): Long {
    return try {
        val parts = trim().split(":").map { it.toLong() }
        when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
            else -> 0L
        }
    } catch (e: NumberFormatException) {
        0L
    }
}

/**
 * Extracts text from a flexColumn entry in search results.
 */
fun JsonObject?.findFlexText(): String? {
    return this
        ?.findObjectWithKey("musicResponsiveListItemFlexColumnRenderer")
        ?.get("text")
        ?.jsonObject
        .getRunsText()
        .takeIf { it != "Unknown" }
}
