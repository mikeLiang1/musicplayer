package org.example.project

import android.content.Context
import android.preference.PreferenceManager
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {

    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        // .cache(Cache(File(context.externalCacheDir, "okhttp"), 16 * 1024 * 1024))
        .build()

    private val mCookies: MutableMap<String, String> = mutableMapOf()

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE_KEY = "youtube_restricted_mode_key"
        const val YOUTUBE_RESTRICTED_MODE_COOKIE = "PREF=f2=8000000"
        const val YOUTUBE_DOMAIN = "youtube.com"

        @Volatile
        private var instance: DownloaderImpl? = null

        /**
         * Recommended to call exactly once in the entire lifetime of the application.
         */
        fun init(builder: OkHttpClient.Builder?): DownloaderImpl {
            val result = DownloaderImpl(builder ?: OkHttpClient.Builder())
            instance = result
            return result
        }

        fun getInstance(): DownloaderImpl {
            return instance
                ?: throw IllegalStateException("DownloaderImpl must be initialized with init() first")
        }
    }

    fun getCookies(url: String): String {
        val youtubeCookie =
            if (url.contains(YOUTUBE_DOMAIN)) getCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY) else null

        // Note: Replace 'ReCaptchaActivity.RECAPTCHA_COOKIES_KEY' with your actual key constant
        val captchaCookie = getCookie("recaptcha_cookies")

        return listOfNotNull(youtubeCookie, captchaCookie)
            .flatMap { it.split("; *") }
            .distinct()
            .joinToString("; ")
    }

    fun getCookie(key: String): String? = mCookies[key]

    fun setCookie(key: String, cookie: String) {
        mCookies[key] = cookie
    }

    fun removeCookie(key: String) {
        mCookies.remove(key)
    }

    fun updateYoutubeRestrictedModeCookies(context: Context) {
        // Ensure you have the string resource or replace with a hardcoded key
        val restrictedModeEnabledKey = "youtube_restricted_mode_enabled"
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val restrictedModeEnabled = prefs.getBoolean(restrictedModeEnabledKey, false)
        updateYoutubeRestrictedModeCookies(restrictedModeEnabled)
    }

    fun updateYoutubeRestrictedModeCookies(youtubeRestrictedModeEnabled: Boolean) {
        if (youtubeRestrictedModeEnabled) {
            setCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY, YOUTUBE_RESTRICTED_MODE_COOKIE)
        } else {
            removeCookie(YOUTUBE_RESTRICTED_MODE_COOKIE_KEY)
        }
    }

    @Throws(IOException::class)
    fun getContentLength(url: String): Long {
        try {
            val response = head(url)
            return response.getHeader("Content-Length")?.toLong()
                ?: throw IOException("Invalid content length")
        } catch (e: NumberFormatException) {
            throw IOException("Invalid content length", e)
        } catch (e: ReCaptchaException) {
            throw IOException(e)
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.let {
            RequestBody.create(null, it) // MediaType is null as default
        }

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        val cookies = getCookies(url)
        if (cookies.isNotEmpty()) {
            requestBuilder.addHeader("Cookie", cookies)
        }

        headers.forEach { (headerName, headerValueList) ->
            requestBuilder.removeHeader(headerName)
            headerValueList.forEach { headerValue ->
                requestBuilder.addHeader(headerName, headerValue)
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBodyToReturn = response.body?.string()
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBodyToReturn,
                latestUrl
            )
        }
    }
}
