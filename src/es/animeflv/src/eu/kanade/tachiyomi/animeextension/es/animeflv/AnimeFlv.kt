package eu.kanade.tachiyomi.animeextension.es.animeflv

import eu.kanade.tachiyomi.animeextension.lib.SibnetExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamWishExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamTapeExtractor
import eu.kanade.tachiyomi.animeextension.lib.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class AnimeFlv {
    val name = "AnimeFLV"
    val baseUrl = "https://animeflv.net"
    val lang = "es"
    val supportsLatest = true

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

    // Popular Anime (Browsing)
    fun getPopularAnime(page: Int = 1): List<AnimeItem> {
        val url = "$baseUrl/browse?order=rating&page=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("ul.ListAnimes li article").map { element ->
            val title = element.select("h3.Title").text()
            val animeUrl = baseUrl + element.select("a").attr("href")
            val thumb = element.select("div.Image figure img").attr("src")
            val fullThumb = if (thumb.startsWith("http")) thumb else "$baseUrl$thumb"
            AnimeItem(title, animeUrl, fullThumb)
        }
    }

    // Search Anime
    fun searchAnime(query: String, page: Int = 1): List<AnimeItem> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/browse?q=$encodedQuery&page=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("ul.ListAnimes li article").map { element ->
            val title = element.select("h3.Title").text()
            val animeUrl = baseUrl + element.select("a").attr("href")
            val thumb = element.select("div.Image figure img").attr("src")
            val fullThumb = if (thumb.startsWith("http")) thumb else "$baseUrl$thumb"
            AnimeItem(title, animeUrl, fullThumb)
        }
    }

    // Latest Episodes
    fun getLatestEpisodes(): List<AnimeItem> {
        val request = Request.Builder().url(baseUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        return doc.select("ul.ListEpis li a").map { element ->
            val title = element.select("strong.Title").text()
            val epNum = element.select("span.Capa").text()
            val fullTitle = "$title - $epNum"
            val epUrl = baseUrl + element.attr("href")
            val thumb = element.select("img").attr("src")
            val fullThumb = if (thumb.startsWith("http")) thumb else "$baseUrl$thumb"
            AnimeItem(fullTitle, epUrl, fullThumb)
        }
    }

    // Episodes List for an Anime Page
    fun getEpisodeList(animeUrl: String): List<EpisodeItem> {
        val request = Request.Builder().url(animeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val episodeList = mutableListOf<EpisodeItem>()
        // Parse JS variables var anime_info = [...] and var episodes = [...]
        val animeId = body.substringAfter("var anime_info = [\"", "").substringBefore("\"", "")
        val animeSlug = body.substringAfter("var anime_info = [\"$animeId\",\"", "").substringBefore("\"", "")

        val episodesData = body.substringAfter("var episodes = [", "").substringBefore("];", "")
        if (episodesData.isNotBlank()) {
            val epPairs = episodesData.split("],[").map {
                it.replace("[", "").replace("]", "").trim()
            }
            epPairs.forEach { pair ->
                val parts = pair.split(",")
                if (parts.size >= 2) {
                    val epNum = parts[0].trim().toFloatOrNull() ?: 1f
                    val epUrl = "$baseUrl/ver/$animeSlug-$epNum"
                    episodeList.add(EpisodeItem("Episodio ${epNum.toInt()}", epUrl, epNum))
                }
            }
        }
        return episodeList
    }

    // Extract Video Servers for Episode
    fun getVideoList(episodeUrl: String): List<Video> {
        val request = Request.Builder().url(episodeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val videos = mutableListOf<Video>()
        val videosScript = body.substringAfter("var videos = ", "").substringBefore(";", "")
        
        if (videosScript.contains("SWISH") || videosScript.contains("streamwish")) {
            val wishUrl = videosScript.substringAfter("streamwish.top/e/", "").substringBefore("\"", "")
            if (wishUrl.isNotBlank()) {
                val extractor = StreamWishExtractor(client)
                videos.addAll(extractor.videosFromUrl("https://streamwish.top/e/$wishUrl"))
            }
        }
        if (videosScript.contains("sibnet.ru")) {
            val sibnetUrl = videosScript.substringAfter("video.sibnet.ru/shell.php?videoid=", "").substringBefore("\"", "")
            if (sibnetUrl.isNotBlank()) {
                val extractor = SibnetExtractor(client)
                videos.addAll(extractor.videosFromUrl("https://video.sibnet.ru/shell.php?videoid=$sibnetUrl"))
            }
        }
        if (videosScript.contains("streamtape.com")) {
            val stUrl = videosScript.substringAfter("streamtape.com/e/", "").substringBefore("\"", "")
            if (stUrl.isNotBlank()) {
                val extractor = StreamTapeExtractor(client)
                videos.addAll(extractor.videosFromUrl("https://streamtape.com/e/$stUrl"))
            }
        }

        return videos
    }
}
