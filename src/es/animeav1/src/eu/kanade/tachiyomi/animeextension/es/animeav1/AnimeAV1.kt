package eu.kanade.tachiyomi.animeextension.es.animeav1

import eu.kanade.tachiyomi.animeextension.lib.SibnetExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamTapeExtractor
import eu.kanade.tachiyomi.animeextension.lib.StreamWishExtractor
import eu.kanade.tachiyomi.animeextension.lib.Video
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class AnimeAV1 {
    val name = "AnimeAV1"
    val baseUrl = "https://animeav1.com"
    val lang = "es"

    private val client = OkHttpClient.Builder().build()
    private val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
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

    // Popular / Catalog Anime
    fun getPopularAnime(page: Int = 1): List<AnimeItem> {
        val url = if (page == 1) "$baseUrl/catalogo" else "$baseUrl/catalogo?page=$page"
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val items = mutableListOf<AnimeItem>()
        doc.select("article").forEach { element ->
            val title = element.select("h3").text().ifBlank { element.select("img").attr("alt") }
            val relUrl = element.select("a[href^=/media/]").attr("href").ifBlank { element.select("a").attr("href") }
            val thumb = element.select("img").attr("src")
            if (title.isNotBlank() && relUrl.contains("/media/")) {
                val fullUrl = if (relUrl.startsWith("http")) relUrl else "$baseUrl$relUrl"
                val fullThumb = if (thumb.startsWith("http")) thumb else "$baseUrl$thumb"
                val cleanTitle = title.replace("^Portada de ".toRegex(), "")
                if (!items.any { it.url == fullUrl }) {
                    items.add(AnimeItem(cleanTitle, fullUrl, fullThumb))
                }
            }
        }
        return items
    }

    // Search Anime
    fun searchAnime(query: String, page: Int = 1): List<AnimeItem> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/catalogo?search=$encodedQuery" + if (page > 1) "&page=$page" else ""
        val request = Request.Builder().url(url).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val items = mutableListOf<AnimeItem>()
        doc.select("article").forEach { element ->
            val title = element.select("h3").text().ifBlank { element.select("img").attr("alt") }
            val relUrl = element.select("a[href^=/media/]").attr("href").ifBlank { element.select("a").attr("href") }
            val thumb = element.select("img").attr("src")
            if (title.isNotBlank() && relUrl.contains("/media/")) {
                val fullUrl = if (relUrl.startsWith("http")) relUrl else "$baseUrl$relUrl"
                val fullThumb = if (thumb.startsWith("http")) thumb else "$baseUrl$thumb"
                val cleanTitle = title.replace("^Portada de ".toRegex(), "")
                if (!items.any { it.url == fullUrl }) {
                    items.add(AnimeItem(cleanTitle, fullUrl, fullThumb))
                }
            }
        }
        return items
    }

    // Episode List
    fun getEpisodeList(animeUrl: String): List<EpisodeItem> {
        val request = Request.Builder().url(animeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return emptyList()

        val animeSlug = animeUrl.substringAfterLast("/media/").substringBefore("/")
        val episodes = mutableListOf<EpisodeItem>()

        // Extracción desde datos Svelte
        val svelteRegex = "episodes:\\[\\{.*?\\}\\]".toRegex()
        val match = svelteRegex.find(body)
        if (match != null) {
            val numRegex = "number:(\\d+)".toRegex()
            numRegex.findAll(match.value).forEach { numMatch ->
                val epNum = numMatch.groupValues[1].toFloatOrNull() ?: 1f
                val epUrl = "$baseUrl/media/$animeSlug/${epNum.toInt()}"
                episodes.add(EpisodeItem("Episodio ${epNum.toInt()}", epUrl, epNum))
            }
        }

        return episodes.sortedByDescending { it.episodeNumber }
    }

    // Videos
    fun getVideoList(episodeUrl: String): List<Video> {
        val request = Request.Builder().url(episodeUrl).headers(headers).build()
        val response = client.newCall(request).execute()
        val doc = Jsoup.parse(response.body?.string() ?: "")

        val videos = mutableListOf<Video>()
        val iframeSrc = doc.select("iframe").attr("src")
        if (iframeSrc.isNotBlank()) {
            videos.add(Video(iframeSrc, "AnimeAV1 Player HD", iframeSrc))
        }

        return videos
    }
}
