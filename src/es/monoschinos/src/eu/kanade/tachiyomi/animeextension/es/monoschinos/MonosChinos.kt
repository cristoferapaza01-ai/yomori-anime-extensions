package eu.kanade.tachiyomi.animeextension.es.monoschinos

import eu.kanade.tachiyomi.animeextension.lib.SibnetExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamTapeExtractor
import eu.kanade.tachiyomi.animeextension.lib.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class MonosChinos {
    val name = "MonosChinos"
    val baseUrl = "https://monoschinos2.com"
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
        val url = "$baseUrl/animes?page=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("div.herobox article.series").map { element ->
            val title = element.select("h3.title").text()
            val animeUrl = element.select("a").attr("href")
            val thumb = element.select("img").attr("src")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Search Anime
    fun searchAnime(query: String, page: Int = 1): List<AnimeItem> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/buscar?q=$encodedQuery&page=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("div.herobox article.series").map { element ->
            val title = element.select("h3.title").text()
            val animeUrl = element.select("a").attr("href")
            val thumb = element.select("img").attr("src")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Episode List
    fun getEpisodeList(animeUrl: String): List<EpisodeItem> {
        val request = Request.Builder().url(animeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val episodes = mutableListOf<EpisodeItem>()
        doc.select("div.episodes-list a").forEachIndexed { index, element ->
            val epTitle = element.select("span").text()
            val epUrl = element.attr("href")
            val epNum = (index + 1).toFloat()
            episodes.add(EpisodeItem(epTitle.ifBlank { "Episodio ${index + 1}" }, epUrl, epNum))
        }

        return episodes.reversed()
    }

    // Videos
    fun getVideoList(episodeUrl: String): List<Video> {
        val request = Request.Builder().url(episodeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val videos = mutableListOf<Video>()
        doc.select("button.play-video").forEach { button ->
            val videoUrl = button.attr("data-player")
            if (videoUrl.contains("sibnet")) {
                val extractor = SibnetExtractor(client)
                videos.addAll(extractor.videosFromUrl(videoUrl))
            } else if (videoUrl.contains("streamtape")) {
                val extractor = StreamTapeExtractor(client)
                videos.addAll(extractor.videosFromUrl(videoUrl))
            }
        }

        return videos
    }
}
