package org.example.project

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

class DownloaderImpl private constructor(builder: OkHttpClient.Builder) : Downloader() {

    private val client: OkHttpClient = builder
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        private const val YOUTUBE_DOMAIN = "youtube.com"

        // Use SOCS cookie instead of CONSENT
        private const val YOUTUBE_CONSENT_COOKIE = "SOCS=CAESEwgDEgk2MTkzNzM5OTAaAmVuIAEaBgiA_LyaBg"

        @Volatile
        private var instance: DownloaderImpl? = null

        fun init(builder: OkHttpClient.Builder? = null): DownloaderImpl {
            val httpLoggingInterceptor = HttpLoggingInterceptor()
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
//            builder?.addInterceptor(httpLoggingInterceptor)
            return DownloaderImpl(builder ?: OkHttpClient.Builder()).also {
                instance = it
            }
        }

        fun getInstance(): DownloaderImpl {
            return instance ?: throw IllegalStateException("Call init() first")
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val requestBody = request.dataToSend()?.let {
            RequestBody.create(null, it)
        }

        val requestBuilder = okhttp3.Request.Builder()
            .method(request.httpMethod(), requestBody)
            .url(request.url())
            .addHeader("User-Agent", USER_AGENT)

        // Add consent cookie for YouTube URLs
        if (request.url().contains(YOUTUBE_DOMAIN)) {
            requestBuilder.addHeader("Cookie", YOUTUBE_CONSENT_COOKIE)
        }

        // Add custom headers from request
        request.headers().forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", request.url())
            }

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString()
            )
        }
    }
}
