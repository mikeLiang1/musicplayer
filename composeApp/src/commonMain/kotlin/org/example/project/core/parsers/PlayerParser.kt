package org.example.project.core.parsers

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses the InnerTube /player response and extracts the best audio-only stream URL.
 *
 * Selection priority:
 * 1. Audio-only formats (no video width)
 * 2. Highest audio quality tier (AUDIO_QUALITY_HIGH > MEDIUM > LOW)
 * 3. Most audio channels (stereo > mono)
 * 4. Highest bitrate
 * 5. Codec preference: OPUS > M4A (minor tiebreaker only)
 *
 * Falls back to signatureCipher / cipher decoding if no direct URL is available.
 */
fun parsePlayerResponse(response: JsonObject): String? {
    val streamingData = response["streamingData"]?.jsonObject ?: return null
    val adaptiveFormats = streamingData["adaptiveFormats"]?.jsonArray ?: return null

    data class AudioFormat(
        val url: String?,
        val signatureCipher: String?,
        val cipher: String?,
        val mimeType: String,
        val bitrate: Int,
        val audioQuality: String?,
        val audioChannels: Int,
    )

    val formats = adaptiveFormats.mapNotNull { format ->
        val obj = format.jsonObject
        val mimeType = obj["mimeType"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val width = obj["width"]?.jsonPrimitive?.intOrNull

        // Skip video formats (those with width)
        if (width != null && width > 0) return@mapNotNull null

        val url = obj["url"]?.jsonPrimitive?.contentOrNull
        val signatureCipher = obj["signatureCipher"]?.jsonPrimitive?.contentOrNull
        val cipher = obj["cipher"]?.jsonPrimitive?.contentOrNull

        // Must have either a URL or a cipher to decode
        if (url == null && signatureCipher == null && cipher == null) return@mapNotNull null

        val bitrate = obj["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
        val audioQuality = obj["audioQuality"]?.jsonPrimitive?.contentOrNull
        val audioChannels = obj["audioChannels"]?.jsonPrimitive?.intOrNull ?: 2

        AudioFormat(
            url = url,
            signatureCipher = signatureCipher,
            cipher = cipher,
            mimeType = mimeType,
            bitrate = bitrate,
            audioQuality = audioQuality,
            audioChannels = audioChannels,
        )
    }

    if (formats.isEmpty()) return null

    // Pick the best format by quality tier, channels, then bitrate.
    // Codec (OPUS vs M4A) is only a minor tiebreaker — bitrate matters more.
    fun qualityTier(q: String?): Int = when (q) {
        "AUDIO_QUALITY_HIGH" -> 3
        "AUDIO_QUALITY_MEDIUM" -> 2
        "AUDIO_QUALITY_LOW" -> 1
        else -> 0
    }

    fun codecOrder(mimeType: String): Int = when {
        mimeType.contains("opus", ignoreCase = true) -> 1
        mimeType.contains("mp4a", ignoreCase = true) -> 0
        else -> -1
    }

    val best = formats.maxWithOrNull(
        compareByDescending<AudioFormat> { qualityTier(it.audioQuality) }
            .thenByDescending { it.audioChannels }
            .thenByDescending { it.bitrate }
            .thenByDescending { codecOrder(it.mimeType) }
    ) ?: return null

    // 1. Direct URL
    best.url?.let { return it }

    // 2. Try signatureCipher (YouTube's obfuscated URL mechanism)
    val cipherText = best.signatureCipher ?: best.cipher
    if (cipherText != null) {
        return decodeSignatureCipher(cipherText)
    }

    return null
}

/**
 * Decodes a YouTube signatureCipher string into a usable stream URL.
 *
 * YouTube wraps some stream URLs in a cipher string like:
 *   sp=sig&url=https://...&s=signature_value
 *
 * The URL needs the `s` (signature) appended as a query param named by `sp`.
 */
internal fun decodeSignatureCipher(cipher: String): String? {
    val params = cipher.split("&").associate { param ->
        val parts = param.split("=", limit = 2)
        parts[0] to (parts.getOrNull(1) ?: "")
    }

    val url = params["url"]?.replace("%3A", ":")
        ?.replace("%2F", "/")
        ?.replace("%3F", "?")
        ?.replace("%3D", "=")
        ?.replace("%26", "&")
        ?: return null

    val s = params["s"]?.replace("%3D", "=")
        ?.replace("%26", "&")
        ?: return null

    val sp = params["sp"] ?: "signature"

    // For common cases where YouTube uses simple signature schemes,
    // the signature is appended as a query parameter.
    // Note: Real signature deciphering requires YouTube's player.js cipher
    // functions (reverse, splice, swap). For now, try the direct approach.
    // If this fails, NewPipe fallback in MediaService handles it.
    val separator = if (url.contains("?")) "&" else "?"
    return "$url$separator$sp=$s"
}
