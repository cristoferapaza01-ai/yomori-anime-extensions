package eu.kanade.tachiyomi.animeextension.lib

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

data class Video(
    val url: String,
    val quality: String,
    val videoUrl: String,
    val headers: Headers? = null
)

class SibnetExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, prefix: String = "Sibnet: "): List<Video> {
        val videoList = mutableListOf<Video>()
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return emptyList()
            val slug = body.substringAfter("player.src([{src: \"", "").substringBefore("\"", "")
            if (slug.isNotBlank()) {
                val videoUrl = if (slug.startsWith("http")) slug else "https://video.sibnet.ru$slug"
                val headers = Headers.Builder().add("Referer", url).build()
                videoList.add(Video(url, "${prefix}720p", videoUrl, headers))
            }
        } catch (_: Exception) {}
        return videoList
    }
}

class StreamWishExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, prefix: String = "StreamWish: "): List<Video> {
        val videoList = mutableListOf<Video>()
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return emptyList()
            val m3u8Url = body.substringAfter("file:\"", "").substringBefore("\"", "")
            if (m3u8Url.isNotBlank() && m3u8Url.contains(".m3u8")) {
                videoList.add(Video(url, "${prefix}Auto", m3u8Url))
            }
        } catch (_: Exception) {}
        return videoList
    }
}

class StreamTapeExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, prefix: String = "StreamTape: "): List<Video> {
        val videoList = mutableListOf<Video>()
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return emptyList()
            val target = "robotlink"
            if (body.contains(target)) {
                val part1 = body.substringAfter("id=\"robotlink\">").substringBefore("<")
                val part2 = body.substringAfter("document.getElementById('robotlink').innerHTML = '").substringBefore("'")
                val videoUrl = "https:" + part1 + part2.substring(3)
                videoList.add(Video(url, "${prefix}720p", videoUrl))
            }
        } catch (_: Exception) {}
        return videoList
    }
}

class VoeExtractor(private val client: OkHttpClient) {
    fun videosFromUrl(url: String, prefix: String = "Voe: "): List<Video> {
        val videoList = mutableListOf<Video>()
        try {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            val body = response.body?.string() ?: return emptyList()
            val hlsUrl = body.substringAfter("'hls': '", "").substringBefore("'", "")
            if (hlsUrl.isNotBlank()) {
                videoList.add(Video(url, "${prefix}HLS", hlsUrl))
            }
        } catch (_: Exception) {}
        return videoList
    }
}
