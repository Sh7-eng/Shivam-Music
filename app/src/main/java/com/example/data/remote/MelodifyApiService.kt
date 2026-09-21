package com.example.data.remote

import android.util.Log
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MelodifyApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val TAG = "MelodifyApiService"

    suspend fun searchSongs(query: String, limit: Int = 15): List<Song> = withContext(Dispatchers.IO) {
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return@withContext emptyList()

        // 1. Try Primary Saavn API
        val primarySongs = searchWithPrimarySaavn(cleanQ, limit)
        if (primarySongs.isNotEmpty()) {
            return@withContext primarySongs
        }

        // 2. Try Direct JioSaavn Search API
        val jioSongs = searchWithDirectJioSaavn(cleanQ, limit)
        if (jioSongs.isNotEmpty()) {
            return@withContext jioSongs
        }

        // 3. Try iTunes Search API
        val itunesSongs = searchWithITunes(cleanQ, limit)
        if (itunesSongs.isNotEmpty()) {
            return@withContext itunesSongs
        }

        // 4. Fallback matching against curated catalog
        val catalogMatches = FEATURED_CHARTS.filter {
            it.name.contains(cleanQ, ignoreCase = true) ||
            it.artist.contains(cleanQ, ignoreCase = true) ||
            it.album.contains(cleanQ, ignoreCase = true)
        }
        catalogMatches
    }

    private fun searchWithPrimarySaavn(query: String, limit: Int): List<Song> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://saavn.sumit.co/api/search/songs?query=$encodedQuery&limit=$limit&page=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string()?.trim() ?: return emptyList()
            if (!body.startsWith("{")) return emptyList()

            val json = JSONObject(body)
            val resultsArray = when {
                json.has("data") && json.optJSONObject("data")?.has("results") == true -> {
                    json.getJSONObject("data").getJSONArray("results")
                }
                json.has("data") && json.optJSONArray("data") != null -> {
                    json.getJSONArray("data")
                }
                json.has("results") -> {
                    json.getJSONArray("results")
                }
                else -> JSONArray()
            }

            val songs = mutableListOf<Song>()
            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val song = normalizeSong(item)
                if (song != null && song.downloadLink.isNotBlank()) {
                    songs.add(song)
                }
            }
            songs
        } catch (e: Exception) {
            Log.w(TAG, "Primary Saavn search unavailable for '$query': ${e.message}")
            emptyList()
        }
    }

    private fun searchWithDirectJioSaavn(query: String, limit: Int): List<Song> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=$limit&p=1&_marker=0&ctx=android&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string()?.trim() ?: return emptyList()
            if (!body.startsWith("{")) return emptyList()

            val json = JSONObject(body)
            val resultsArray = json.optJSONArray("results") ?: JSONArray()
            val songs = mutableListOf<Song>()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val id = item.optString("id", "")
                if (id.isBlank()) continue

                val rawName = item.optString("song", item.optString("title", ""))
                val name = unescapeHtml(rawName)
                if (name.isBlank()) continue

                val rawArtist = item.optString("primary_artists", item.optString("singers", item.optString("music", "Unknown Artist")))
                val artist = unescapeHtml(rawArtist)
                val album = unescapeHtml(item.optString("album", "Single"))

                val rawImage = item.optString("image", "")
                val art = rawImage
                    .replace("150x150", "500x500")
                    .replace("50x50", "500x500")

                // Media URL: media_preview_url is a direct 96kbps MP4/AAC stream hosted on Saavn CDN
                val downloadLink = item.optString("media_preview_url", "")
                val duration = item.optString("duration", "180").toIntOrNull() ?: 180

                if (downloadLink.isNotBlank()) {
                    songs.add(
                        Song(
                            id = id,
                            name = name,
                            artist = artist,
                            album = album,
                            art = art,
                            downloadLink = downloadLink,
                            duration = duration
                        )
                    )
                }
            }
            songs
        } catch (e: Exception) {
            Log.w(TAG, "Direct JioSaavn search unavailable for '$query': ${e.message}")
            emptyList()
        }
    }

    private fun searchWithITunes(query: String, limit: Int): List<Song> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MelodifyMusic/1.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()

            val body = response.body?.string()?.trim() ?: return emptyList()
            if (!body.startsWith("{")) return emptyList()

            val json = JSONObject(body)
            val resultsArray = json.optJSONArray("results") ?: JSONArray()
            val songs = mutableListOf<Song>()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.optJSONObject(i) ?: continue
                val trackId = item.optString("trackId", item.optString("id", ""))
                val trackName = item.optString("trackName", "")
                if (trackName.isBlank()) continue

                val artistName = item.optString("artistName", "Unknown Artist")
                val collectionName = item.optString("collectionName", "Single")
                val previewUrl = item.optString("previewUrl", "")
                if (previewUrl.isBlank()) continue

                val rawArt = item.optString("artworkUrl100", item.optString("artworkUrl60", ""))
                val art = rawArt.replace("100x100bb.jpg", "600x600bb.jpg")
                val durationMs = item.optLong("trackTimeMillis", 180000L)
                val durationSec = (durationMs / 1000).toInt()

                songs.add(
                    Song(
                        id = if (trackId.isNotBlank()) "itunes_$trackId" else "itunes_${System.currentTimeMillis()}_$i",
                        name = trackName,
                        artist = artistName,
                        album = collectionName,
                        art = art,
                        downloadLink = previewUrl,
                        duration = if (durationSec > 0) durationSec else 180
                    )
                )
            }
            songs
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search unavailable for '$query': ${e.message}")
            emptyList()
        }
    }

    suspend fun getSongById(id: String): Song? = withContext(Dispatchers.IO) {
        if (id.isBlank()) return@withContext null
        try {
            val url = "https://saavn.sumit.co/api/songs/$id"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val fallbackResults = searchSongs(id, limit = 1)
                return@withContext fallbackResults.firstOrNull()
            }

            val body = response.body?.string()?.trim() ?: return@withContext null
            if (!body.startsWith("{")) {
                val fallbackResults = searchSongs(id, limit = 1)
                return@withContext fallbackResults.firstOrNull()
            }

            val json = JSONObject(body)
            val item = when {
                json.has("data") && json.optJSONArray("data") != null -> {
                    json.getJSONArray("data").optJSONObject(0)
                }
                json.has("data") && json.optJSONObject("data") != null -> {
                    json.getJSONObject("data")
                }
                else -> null
            }

            if (item != null) {
                normalizeSong(item)
            } else {
                // Fallback to search query by id
                val fallbackResults = searchSongs(id, limit = 1)
                fallbackResults.firstOrNull()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching song by ID '$id': ${e.message}")
            val fallbackResults = searchSongs(id, limit = 1)
            fallbackResults.firstOrNull()
        }
    }

    suspend fun getLyrics(trackName: String, artistName: String, albumName: String, durationSec: Int): LyricsData =
        withContext(Dispatchers.IO) {
            // 1. Check curated lyrics for immediate, high-fidelity real-time sync
            val curated = CuratedLyrics.getCuratedLyrics(trackName, artistName)
            if (!curated.isNullOrBlank()) {
                val lines = parseLrc(curated)
                if (lines.isNotEmpty()) {
                    return@withContext LyricsData(
                        isSynced = true,
                        lines = lines,
                        plainLyrics = lines.joinToString("\n") { it.text },
                        source = "Shivam High-Fi Sync"
                    )
                }
            }

            try {
                val cleanTrack = cleanTitle(trackName)
                val cleanArtist = cleanArtist(artistName)

                val encTrack = URLEncoder.encode(cleanTrack, "UTF-8")
                val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")
                val encAlbum = URLEncoder.encode(albumName, "UTF-8")

                // Strategy 1: Exact match with clean metadata
                val exactUrl = "https://lrclib.net/api/get?track_name=$encTrack&artist_name=$encArtist" +
                        if (albumName.isNotBlank()) "&album_name=$encAlbum" else "" +
                        if (durationSec > 0) "&duration=$durationSec" else ""

                val req1 = Request.Builder()
                    .url(exactUrl)
                    .header("User-Agent", "ShivamMusic/1.0 (https://github.com/shivam)")
                    .build()
                val resp1 = client.newCall(req1).execute()
                if (resp1.isSuccessful) {
                    val body = resp1.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseLyricsJson(body, durationSec)
                        if (parsed != null && (parsed.isSynced || !parsed.plainLyrics.isNullOrBlank())) {
                            return@withContext parsed
                        }
                    }
                }

                // Strategy 2: Exact match without album filter
                val exactNoAlbumUrl = "https://lrclib.net/api/get?track_name=$encTrack&artist_name=$encArtist"
                val req2 = Request.Builder()
                    .url(exactNoAlbumUrl)
                    .header("User-Agent", "ShivamMusic/1.0 (https://github.com/shivam)")
                    .build()
                val resp2 = client.newCall(req2).execute()
                if (resp2.isSuccessful) {
                    val body = resp2.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseLyricsJson(body, durationSec)
                        if (parsed != null && (parsed.isSynced || !parsed.plainLyrics.isNullOrBlank())) {
                            return@withContext parsed
                        }
                    }
                }

                // Strategy 3: Fallback search query
                val searchQ = URLEncoder.encode("$cleanTrack $cleanArtist", "UTF-8")
                val searchUrl = "https://lrclib.net/api/search?q=$searchQ"
                val req3 = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "ShivamMusic/1.0 (https://github.com/shivam)")
                    .build()
                val resp3 = client.newCall(req3).execute()
                if (resp3.isSuccessful) {
                    val body = resp3.body?.string()
                    if (!body.isNullOrBlank()) {
                        val array = JSONArray(body)
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val parsed = parseLyricsJsonObject(obj, durationSec)
                            if (parsed.isSynced || !parsed.plainLyrics.isNullOrBlank()) {
                                return@withContext parsed
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching lyrics", e)
            }
            LyricsData(
                isSynced = false,
                lines = emptyList(),
                plainLyrics = "No lyrics found for this track. Tap Refresh to try again.",
                source = "Offline"
            )
        }

    private fun cleanTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\((feat|with|from|official|remix|audio|video|lyric).*?\\)"), "")
            .replace(Regex("(?i)\\[(feat|with|from|official|remix|audio|video|lyric).*?\\]"), "")
            .replace(Regex("(?i)- (single|ep|soundtrack|from .*?)"), "")
            .trim()
    }

    private fun cleanArtist(artist: String): String {
        return artist
            .split(",", "&", "feat.", "ft.", "/")
            .firstOrNull()
            ?.trim() ?: artist.trim()
    }

    private fun parseLyricsJson(jsonStr: String, durationSec: Int = 180): LyricsData? {
        return try {
            val obj = JSONObject(jsonStr)
            parseLyricsJsonObject(obj, durationSec)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLyricsJsonObject(obj: JSONObject, durationSec: Int = 180): LyricsData {
        val synced = obj.optString("syncedLyrics", "")
        val plain = obj.optString("plainLyrics", "")

        if (synced.isNotBlank()) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) {
                return LyricsData(
                    isSynced = true,
                    lines = lines,
                    plainLyrics = plain.ifBlank { lines.joinToString("\n") { it.text } },
                    source = "LRCLIB Real-time Sync"
                )
            }
        }

        if (plain.isNotBlank()) {
            val rawPlainLines = plain.split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // Synthesize realistic timestamps if only plain text is available
            val totalDurationMs = if (durationSec > 0) durationSec * 1000L else 180000L
            val intervalMs = if (rawPlainLines.isNotEmpty()) {
                (totalDurationMs / (rawPlainLines.size + 1)).coerceIn(2500L, 5000L)
            } else 4000L

            val syncedLines = rawPlainLines.mapIndexed { index, text ->
                val start = index * intervalMs
                LyricLine(
                    timeMs = start,
                    text = text,
                    endTimeMs = start + intervalMs
                )
            }
            return LyricsData(
                isSynced = true,
                lines = syncedLines,
                plainLyrics = plain,
                source = "Auto-Paced Sync"
            )
        }

        return LyricsData(isSynced = false, lines = emptyList(), plainLyrics = null)
    }

    private fun parseLrc(lrcContent: String): List<LyricLine> {
        val rawLines = mutableListOf<LyricLine>()
        val pattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

        for (line in lrcContent.split("\n")) {
            val trimmed = line.trim()
            val matcher = pattern.matcher(trimmed)
            if (matcher.matches()) {
                try {
                    val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                    val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                    val fractionStr = matcher.group(3) ?: "0"
                    val millis = if (fractionStr.length == 2) {
                        (fractionStr.toLongOrNull() ?: 0L) * 10
                    } else {
                        fractionStr.toLongOrNull() ?: 0L
                    }
                    val totalMs = (minutes * 60 + seconds) * 1000 + millis
                    val text = matcher.group(4)?.trim() ?: ""
                    if (text.isNotBlank()) {
                        rawLines.add(LyricLine(timeMs = totalMs, text = text))
                    }
                } catch (e: Exception) {
                    // ignore malformed line
                }
            }
        }
        val sorted = rawLines.sortedBy { it.timeMs }
        return sorted.mapIndexed { index, item ->
            val nextStart = if (index + 1 < sorted.size) sorted[index + 1].timeMs else item.timeMs + 5000L
            item.copy(endTimeMs = nextStart)
        }
    }

    private fun normalizeSong(item: JSONObject): Song? {
        val id = item.optString("id", "")
        if (id.isBlank()) return null

        val rawName = item.optString("name", item.optString("title", "Unknown Track"))
        val name = unescapeHtml(rawName)

        val artist = extractArtist(item)
        val album = extractAlbum(item)
        val art = extractBestImage(item)
        val downloadLink = extractBestAudio(item)

        val duration = when {
            item.has("duration") -> item.optInt("duration", 180)
            item.has("duration_ms") -> (item.optLong("duration_ms", 180000) / 1000).toInt()
            else -> 180
        }

        return Song(
            id = id,
            name = name,
            artist = artist,
            album = album,
            art = art,
            downloadLink = downloadLink,
            duration = if (duration > 0) duration else 180
        )
    }

    private fun extractArtist(item: JSONObject): String {
        // primaryArtists string
        val primaryArtists = item.optString("primaryArtists", "")
        if (primaryArtists.isNotBlank()) return unescapeHtml(primaryArtists)

        // artists object or array
        val artistsObj = item.optJSONObject("artists")
        if (artistsObj != null) {
            val primaryArr = artistsObj.optJSONArray("primary")
            if (primaryArr != null && primaryArr.length() > 0) {
                val names = mutableListOf<String>()
                for (i in 0 until primaryArr.length()) {
                    val a = primaryArr.optJSONObject(i)
                    if (a != null && a.has("name")) {
                        names.add(a.optString("name"))
                    } else if (primaryArr.optString(i).isNotBlank()) {
                        names.add(primaryArr.optString(i))
                    }
                }
                if (names.isNotEmpty()) return unescapeHtml(names.joinToString(", "))
            }
        }

        val artistStr = item.optString("artist", "")
        if (artistStr.isNotBlank()) return unescapeHtml(artistStr)

        return "Unknown Artist"
    }

    private fun extractAlbum(item: JSONObject): String {
        val albumObj = item.optJSONObject("album")
        if (albumObj != null) {
            val name = albumObj.optString("name", "")
            if (name.isNotBlank()) return unescapeHtml(name)
        }
        val albumStr = item.optString("album", "")
        if (albumStr.isNotBlank()) return unescapeHtml(albumStr)
        return "Unknown Album"
    }

    private fun extractBestImage(item: JSONObject): String {
        val imageArray = item.optJSONArray("image") ?: item.optJSONArray("images")
        if (imageArray != null && imageArray.length() > 0) {
            // Find 500x500 first
            for (i in 0 until imageArray.length()) {
                val img = imageArray.optJSONObject(i)
                if (img != null && img.optString("quality") == "500x500") {
                    val url = img.optString("url", img.optString("link", ""))
                    if (url.isNotBlank()) return url
                }
            }
            // Return last image
            val last = imageArray.optJSONObject(imageArray.length() - 1)
            if (last != null) {
                val url = last.optString("url", last.optString("link", ""))
                if (url.isNotBlank()) return url
            }
            val str = imageArray.optString(imageArray.length() - 1, "")
            if (str.isNotBlank()) return str
        }

        val direct = item.optString("image", item.optString("art", ""))
        return direct
    }

    private fun extractBestAudio(item: JSONObject): String {
        val downloadUrl = item.optJSONArray("downloadUrl")
            ?: item.optJSONArray("download_url")
            ?: item.optJSONArray("media_url")

        if (downloadUrl != null && downloadUrl.length() > 0) {
            val priorities = listOf("320kbps", "160kbps", "96kbps", "48kbps", "12kbps")
            for (quality in priorities) {
                for (i in 0 until downloadUrl.length()) {
                    val obj = downloadUrl.optJSONObject(i)
                    if (obj != null && obj.optString("quality").equals(quality, ignoreCase = true)) {
                        val link = obj.optString("url", obj.optString("link", ""))
                        if (link.isNotBlank()) return link
                    }
                }
            }
            // fallback to last
            val last = downloadUrl.optJSONObject(downloadUrl.length() - 1)
            if (last != null) {
                val link = last.optString("url", last.optString("link", ""))
                if (link.isNotBlank()) return link
            }
            val lastStr = downloadUrl.optString(downloadUrl.length() - 1, "")
            if (lastStr.isNotBlank()) return lastStr
        }

        val direct = item.optString("download_link", item.optString("media_url", ""))
        return direct
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .trim()
    }

    companion object {
        val FEATURED_CHARTS = listOf(
            Song(
                id = "featured_1",
                name = "Blinding Lights",
                artist = "The Weeknd",
                album = "After Hours",
                art = "https://c.saavncdn.com/188/After-Hours-English-2020-20200320002130-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/188/9df55d9d72dfa4f6d4d6cf50fa3f6782_160.mp4",
                duration = 200
            ),
            Song(
                id = "featured_2",
                name = "Shape of You",
                artist = "Ed Sheeran",
                album = "÷ (Divide)",
                art = "https://c.saavncdn.com/129/Shape-of-You-English-2017-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/129/bfb65b6f3c4db618e7c10b784a0c897f_160.mp4",
                duration = 233
            ),
            Song(
                id = "featured_3",
                name = "Starboy",
                artist = "The Weeknd, Daft Punk",
                album = "Starboy",
                art = "https://c.saavncdn.com/492/Starboy-English-2016-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/492/c8ea0f898168a52e9352eef9e4f585d6_160.mp4",
                duration = 230
            ),
            Song(
                id = "featured_4",
                name = "Levitating",
                artist = "Dua Lipa",
                album = "Future Nostalgia",
                art = "https://c.saavncdn.com/431/Future-Nostalgia-English-2020-20200326162335-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/431/9f0df8eefb27cf17eb0d39e3ec011d33_160.mp4",
                duration = 203
            ),
            Song(
                id = "featured_5",
                name = "Kesariya",
                artist = "Arijit Singh, Pritam, Amitabh Bhattacharya",
                album = "Brahmastra",
                art = "https://c.saavncdn.com/191/Kesariya-From-Brahmastra-Hindi-2022-20220717092820-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/191/6ca651631e40ebfa573e04cfb2c2c088_160.mp4",
                duration = 268
            ),
            Song(
                id = "featured_6",
                name = "Stay",
                artist = "The Kid LAROI, Justin Bieber",
                album = "F*CK LOVE 3: OVER YOU",
                art = "https://c.saavncdn.com/396/Stay-English-2021-20210709054707-500x500.jpg",
                downloadLink = "https://aac.saavncdn.com/396/6d6c6e737c3da46927be6dcbf7cf533b_160.mp4",
                duration = 141
            )
        )
    }
}
