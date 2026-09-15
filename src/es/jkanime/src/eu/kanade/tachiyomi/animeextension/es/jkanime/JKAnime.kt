package eu.kanade.tachiyomi.animeextension.es.jkanime

import eu.kanade.tachiyomi.animeextension.lib.SibnetExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamTapeExtractor
import eu.kanade.tachiyomi.animeextension.lib.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class JKAnime {
    val name = "JKAnime"
    val baseUrl = "https://jkanime.net"
    val lang = "es"

    private val client = OkHttpClient.Builder().build()
    private val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Referer", baseUrl)
        .build()

    data class AnimeItem(
        val title: String,
        val url: String,
        val thumbnailUrl: String
    )

    data class EpisodeItem(
        val name: String,
        val url: String,
        val episodeNumber: Float
    )

    // Popular Anime
    fun getPopularAnime(page: Int = 1): List<AnimeItem> {
        val url = "$baseUrl/directorio/$page/"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("div.anime__item").map { element ->
            val title = element.select("div.anime__item__text h5 a").text()
            val animeUrl = element.select("div.anime__item__text h5 a").attr("href")
            val thumb = element.select("div.anime__item__pic").attr("data-setbg")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Search Anime
    fun searchAnime(query: String, page: Int = 1): List<AnimeItem> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/buscar/$encodedQuery/$page/"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("div.anime__item").map { element ->
            val title = element.select("div.anime__item__text h5 a").text()
            val animeUrl = element.select("div.anime__item__text h5 a").attr("href")
            val thumb = element.select("div.anime__item__pic").attr("data-setbg")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Latest Episodes
    fun getLatestEpisodes(): List<AnimeItem> {
        val request = Request.Builder().url(baseUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("div.list__episode div.anime__item").map { element ->
            val title = element.select("div.anime__item__text h5 a").text()
            val epUrl = element.select("div.anime__item__text h5 a").attr("href")
            val thumb = element.select("div.anime__item__pic").attr("data-setbg")
            AnimeItem(title, epUrl, thumb)
        }
    }

    // Episode List
    fun getEpisodeList(animeUrl: String): List<EpisodeItem> {
        val request = Request.Builder().url(animeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val episodes = mutableListOf<EpisodeItem>()
        // JKAnime list structure
        val epElements = doc.select("div.anime__pagination a")
        epElements.forEach { element ->
            val epNumText = element.text()
            val epNum = epNumText.toFloatOrNull() ?: 1f
            val epUrl = element.attr("href")
            episodes.add(EpisodeItem("Episodio ${epNum.toInt()}", epUrl, epNum))
        }

        return episodes.reversed()
    }

    // Videos
    fun getVideoList(episodeUrl: String): List<Video> {
        val request = Request.Builder().url(episodeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val videos = mutableListOf<Video>()
        val iframeSrcs = doc.select("iframe").map { it.attr("src") }

        iframeSrcs.forEach { src ->
            if (src.contains("sibnet")) {
                val extractor = SibnetExtractor(client)
                videos.addAll(extractor.videosFromUrl(src))
            } else if (src.contains("streamtape")) {
                val extractor = StreamTapeExtractor(client)
                videos.addAll(extractor.videosFromUrl(src))
            }
        }

        return videos
    }
}
