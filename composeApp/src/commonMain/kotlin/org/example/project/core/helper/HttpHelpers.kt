package org.example.project.core.helper

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.example.project.core.repository.InnerTubeRepository.Companion.CLIENT_VERSION

fun HttpRequestBuilder.applyHeaders() {
    header("X-YouTube-Client-Name", "67")
    header("X-YouTube-Client-Version", CLIENT_VERSION)
    header(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    )
    header("Referer", "https://music.youtube.com/")
    header("Origin", "https://music.youtube.com")
}

fun buildNextBody(videoId: String): JsonObject = buildJsonObject {
    putJsonObject("context") {
        putJsonObject("client") {
            put("clientName", "WEB_REMIX")
            put("clientVersion", CLIENT_VERSION)
            put("hl", "en")
            put("gl", "US")
            put(
                "userAgent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36,gzip(gfe)"
            )
            put("platform", "DESKTOP")
        }
    }
    put("videoId", videoId)
    put("playlistId", "RDAMVM$videoId")
    put("params", "wAEB") // returns full radio queue
}

fun buildSearchBody(query: String): JsonObject = buildJsonObject {
    putJsonObject("context") {
        putJsonObject("client") {
            put("clientName", "WEB_REMIX")
            put("clientVersion", CLIENT_VERSION)
            put("hl", "en")
            put("gl", "US")
        }
    }
    put("query", query)
    put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D") // music songs filter
}

// Shared between queue pagination and search pagination
fun buildContinuationBody(token: String): JsonObject = buildJsonObject {
    putJsonObject("context") {
        putJsonObject("client") {
            put("clientName", "WEB_REMIX")
            put("clientVersion", CLIENT_VERSION)
            put("hl", "en")
            put("gl", "US")
        }
    }
    put("continuation", token)
}
