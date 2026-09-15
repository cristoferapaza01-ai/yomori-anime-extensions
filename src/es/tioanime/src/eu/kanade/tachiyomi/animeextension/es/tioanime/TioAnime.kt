package eu.kanade.tachiyomi.animeextension.es.tioanime

import eu.kanade.tachiyomi.animeextension.lib.SibnetExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamTapeExtractor
import eu.kanade.tachiyomi.animeextension.lib.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class TioAnime {
    val name = "TioAnime"
    val baseUrl = "https://tioanime.com"
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
        val url = "$baseUrl/directorio?p=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("ul.animes li.anime").map { element ->
            val title = element.select("h3.title").text()
            val animeUrl = baseUrl + element.select("a").attr("href")
            val thumb = baseUrl + element.select("div.thumb figure img").attr("src")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Search Anime
    fun searchAnime(query: String, page: Int = 1): List<AnimeItem> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/directorio?q=$encodedQuery&p=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("ul.animes li.anime").map { element ->
            val title = element.select("h3.title").text()
            val animeUrl = baseUrl + element.select("a").attr("href")
            val thumb = baseUrl + element.select("div.thumb figure img").attr("src")
            AnimeItem(title, animeUrl, thumb)
        }
    }

    // Episode List
    fun getEpisodeList(animeUrl: String): List<EpisodeItem> {
        val request = Request.Builder().url(animeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val animeSlug = animeUrl.substringAfterLast("/")
        val episodes = mutableListOf<EpisodeItem>()

        val episodesData = body.substringAfter("var episodes = [", "").substringBefore("];", "")
        if (episodesData.isNotBlank()) {
            val epNums = episodesData.split(",").mapNotNull { it.trim().toFloatOrNull() }
            epNums.forEach { num ->
                val epUrl = "$baseUrl/ver/$animeSlug-${num.toInt()}"
                episodes.add(EpisodeItem("Episodio ${num.toInt()}", epUrl, num))
            }
        }

        return episodes
    }

    // Videos
    fun getVideoList(episodeUrl: String): List<Video> {
        val request = Request.Builder().url(episodeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val videos = mutableListOf<Video>()
        val videosData = body.substringAfter("var videos = [", "").substringBefore("];", "")
        
        if (videosData.contains("sibnet")) {
            val sibnetUrl = videosData.substringAfter("sibnet.ru/shell.php?videoid=", "").substringBefore("\"", "")
            if (sibnetUrl.isNotBlank()) {
                val extractor = SibnetExtractor(client)
                videos.addAll(extractor.videosFromUrl("https://video.sibnet.ru/shell.php?videoid=$sibnetUrl"))
            }
        }

        return videos
    }
}
