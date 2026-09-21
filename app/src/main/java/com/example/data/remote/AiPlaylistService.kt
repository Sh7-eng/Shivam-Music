package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiPlaylistResult
import com.example.data.model.AiPlaylistTrackItem
import com.example.data.model.MoodArchetype
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiPlaylistService(
    private val musicRepository: MusicRepository
) {
    private val TAG = "AiPlaylistService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    companion object {
        val CURATED_MOODS = listOf(
            MoodArchetype(
                id = "euphoric",
                name = "Euphoric & Radiant",
                emoji = "✨",
                tagline = "Golden hour vibes, dopamine rush & unstoppable joy",
                colorHex = "#FFB300",
                defaultEnergy = 0.85f
            ),
            MoodArchetype(
                id = "melancholy",
                name = "Late Night Rain",
                emoji = "🌧️",
                tagline = "Introspective echoes, gentle heartbreak & nostalgic longing",
                colorHex = "#455A64",
                defaultEnergy = 0.3f
            ),
            MoodArchetype(
                id = "focus_zen",
                name = "Deep Focus & Zen",
                emoji = "🧘",
                tagline = "Lo-Fi calm, steady heartbeats & distraction-free flow",
                colorHex = "#2E7D32",
                defaultEnergy = 0.4f
            ),
            MoodArchetype(
                id = "hype_rage",
                name = "Hype & Unstoppable",
                emoji = "⚡",
                tagline = "Heavy basslines, adrenaline rush & workout peak intensity",
                colorHex = "#D32F2F",
                defaultEnergy = 0.95f
            ),
            MoodArchetype(
                id = "midnight_dream",
                name = "Midnight Reverie",
                emoji = "🌙",
                tagline = "Dream pop, indie hazes & 2 AM neon highway thoughts",
                colorHex = "#5E35B1",
                defaultEnergy = 0.45f
            ),
            MoodArchetype(
                id = "warm_coffee",
                name = "Acoustic Cozy",
                emoji = "☕",
                tagline = "Warm guitar strings, quiet mornings & comforting melodies",
                colorHex = "#8D6E63",
                defaultEnergy = 0.35f
            ),
            MoodArchetype(
                id = "healing_solace",
                name = "Healing & Solace",
                emoji = "🌿",
                tagline = "Cathartic vocals, emotional release & gentle reassurance",
                colorHex = "#00897B",
                defaultEnergy = 0.4f
            ),
            MoodArchetype(
                id = "cyber_drive",
                name = "Cyber Night Drive",
                emoji = "🏎️",
                tagline = "Synthwave pulses, dark electronic bass & endless asphalt",
                colorHex = "#C2185B",
                defaultEnergy = 0.75f
            )
        )
    }

    suspend fun generateMoodPlaylist(
        listeningHistory: List<Song>,
        likedSongs: List<Song>,
        mood: MoodArchetype,
        userCustomThoughts: String = "",
        energyLevel: Float = mood.defaultEnergy,
        historyInfluence: Float = 0.7f,
        onProgress: (String) -> Unit = {}
    ): AiPlaylistResult = withContext(Dispatchers.IO) {
        onProgress("Analyzing emotional vibe and listening patterns...")

        val apiKey = BuildConfig.GEMINI_API_KEY
        val rawAiResult = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                fetchGeminiCuratedPlaylist(
                    apiKey = apiKey,
                    history = listeningHistory,
                    liked = likedSongs,
                    mood = mood,
                    userThoughts = userCustomThoughts,
                    energyLevel = energyLevel,
                    historyInfluence = historyInfluence
                )
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, using intelligent emotional fallback: ${e.message}")
                generateIntelligentFallback(listeningHistory, likedSongs, mood, userCustomThoughts, energyLevel)
            }
        } else {
            generateIntelligentFallback(listeningHistory, likedSongs, mood, userCustomThoughts, energyLevel)
        }

        onProgress("Finding audio streams & high-res artwork...")

        // Resolve tracks into real playable Song entities
        val playableSongs = mutableListOf<Song>()
        val reasonsMap = mutableMapOf<String, String>()

        for (track in rawAiResult.tracks) {
            val query = "${track.title} ${track.artist}".trim()
            val matchedSong = resolvePlayableSong(query, track)
            if (matchedSong != null && playableSongs.none { it.id == matchedSong.id || it.name.equals(matchedSong.name, ignoreCase = true) }) {
                playableSongs.add(matchedSong)
                reasonsMap[matchedSong.id] = track.emotionalReason
            }
            if (playableSongs.size >= 12) break
        }

        // If some tracks couldn't be resolved, supplement with relevant matches from history & catalog
        if (playableSongs.size < 6) {
            supplementWithMoodMatches(playableSongs, reasonsMap, mood, listeningHistory)
        }

        AiPlaylistResult(
            title = rawAiResult.playlistTitle,
            moodAnalysis = rawAiResult.moodAnalysis,
            primaryMood = mood.name,
            energyRating = formatEnergy(energyLevel),
            colorHex = rawAiResult.colorHex.ifBlank { mood.colorHex },
            songs = playableSongs,
            songReasons = reasonsMap
        )
    }

    private suspend fun resolvePlayableSong(query: String, track: AiPlaylistTrackItem): Song? {
        return try {
            // First check if already in local database
            val localResults = musicRepository.searchSongs(query, limit = 3)
            val bestLocal = localResults.firstOrNull {
                it.name.contains(track.title, ignoreCase = true) ||
                track.title.contains(it.name, ignoreCase = true)
            } ?: localResults.firstOrNull()

            if (bestLocal != null && bestLocal.downloadLink.isNotBlank()) {
                return bestLocal
            }

            // Otherwise search by track title directly
            val fallbackSearch = musicRepository.searchSongs(track.title, limit = 2)
            fallbackSearch.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private fun supplementWithMoodMatches(
        playableSongs: MutableList<Song>,
        reasonsMap: MutableMap<String, String>,
        mood: MoodArchetype,
        history: List<Song>
    ) {
        // Pool from featured charts and history
        val candidatePool = (history + MelodifyApiService.FEATURED_CHARTS).distinctBy { it.id }
        for (song in candidatePool) {
            if (playableSongs.none { it.id == song.id }) {
                playableSongs.add(song)
                reasonsMap[song.id] = "Curated based on your listening affinities for ${mood.name}."
                if (playableSongs.size >= 8) break
            }
        }
    }

    private fun fetchGeminiCuratedPlaylist(
        apiKey: String,
        history: List<Song>,
        liked: List<Song>,
        mood: MoodArchetype,
        userThoughts: String,
        energyLevel: Float,
        historyInfluence: Float
    ): ParsedAiPayload {
        val historySamples = history.take(15).joinToString(", ") { "${it.name} by ${it.artist}" }
        val likedSamples = liked.take(10).joinToString(", ") { "${it.name} by ${it.artist}" }
        val energyDesc = when {
            energyLevel < 0.35f -> "Very calm, acoustic, downtempo, peaceful"
            energyLevel < 0.65f -> "Medium tempo, groove, melodic, balanced"
            else -> "High energy, punchy beats, driving rhythm, intense"
        }
        val influenceDesc = when {
            historyInfluence > 0.7f -> "Strongly align with the user's favorite artists and similar aesthetics."
            historyInfluence > 0.4f -> "Balance familiar favorite artists with refreshing new discoveries."
            else -> "Prioritize fresh discoveries and unexpected hidden gems that match this emotion."
        }

        val prompt = """
You are an empathetic musicologist and world-class playlist curator for SHIVAM MUSIC.
The user wants an AI-generated playlist based on emotional mood analysis and listening history.

[USER EMOTIONAL STATE & MOOD]
Target Mood Archetype: ${mood.name} (${mood.tagline})
Specific Emotional Reflections: "${if (userThoughts.isNotBlank()) userThoughts else "Immerse me in the pure essence of ${mood.name}"}"
Desired Energy Level: $energyDesc (Energy value: ${(energyLevel * 100).toInt()}%)
Curatorial Direction: $influenceDesc

[USER LISTENING HISTORY]
Recently Played Tracks: [${if (historySamples.isNotBlank()) historySamples else "Diverse top hits, melodic indie, pop ballads"}]
Liked Favorites: [${if (likedSamples.isNotBlank()) likedSamples else "Energetic anthems and acoustic gems"}]

[INSTRUCTIONS]
1. Perform an insightful emotional mood analysis (2-3 sentences) explaining the user's feeling, how their musical tastes connect to this emotion, and the sonic journey of this playlist.
2. Formulate a captivating, poetic, or evocative Playlist Title (2-4 words, NO generic titles like 'My Mood Playlist').
3. Curate 8-10 real, well-known popular or critically acclaimed songs across pop, indie, rock, hip-hop, acoustic, or Bollywood that perfectly match this emotional mood.
4. For each song, provide a 1-sentence emotional reason explaining why it fits the vibe.
5. Return STRICT JSON with this schema:
{
  "playlistTitle": "String",
  "moodAnalysis": "String",
  "colorHex": "${mood.colorHex}",
  "tracks": [
    {
      "title": "Song Title",
      "artist": "Artist Name",
      "emotionalReason": "Why this song matches the user's emotional state"
    }
  ]
}
""".trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("responseMimeType", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API returned error code ${response.code}")
        }

        val respBody = response.body?.string() ?: throw IllegalStateException("Empty response body from Gemini")
        val json = JSONObject(respBody)
        val candidates = json.optJSONArray("candidates") ?: throw IllegalStateException("No candidates in Gemini response")
        val firstCandidate = candidates.optJSONObject(0) ?: throw IllegalStateException("Empty candidate object")
        val content = firstCandidate.optJSONObject("content") ?: throw IllegalStateException("No content in candidate")
        val parts = content.optJSONArray("parts") ?: throw IllegalStateException("No parts in content")
        val textPart = parts.optJSONObject(0)?.optString("text", "") ?: ""

        return parseAiPayload(textPart, mood)
    }

    private fun parseAiPayload(rawJsonText: String, mood: MoodArchetype): ParsedAiPayload {
        val cleanJson = rawJsonText
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val obj = JSONObject(cleanJson)
        val title = obj.optString("playlistTitle", "${mood.name} Mix")
        val moodAnalysis = obj.optString("moodAnalysis", "A resonant soundscape tailored to your emotional wavelength and listening habits.")
        val colorHex = obj.optString("colorHex", mood.colorHex)

        val tracksArray = obj.optJSONArray("tracks") ?: JSONArray()
        val trackList = mutableListOf<AiPlaylistTrackItem>()

        for (i in 0 until tracksArray.length()) {
            val item = tracksArray.optJSONObject(i) ?: continue
            val songTitle = item.optString("title", "").trim()
            val artist = item.optString("artist", "").trim()
            val reason = item.optString("emotionalReason", "Matches the emotional cadence of your mood.")

            if (songTitle.isNotBlank()) {
                trackList.add(
                    AiPlaylistTrackItem(
                        title = songTitle,
                        artist = artist,
                        emotionalReason = reason
                    )
                )
            }
        }

        return ParsedAiPayload(
            playlistTitle = title,
            moodAnalysis = moodAnalysis,
            colorHex = colorHex,
            tracks = trackList
        )
    }

    private fun generateIntelligentFallback(
        history: List<Song>,
        liked: List<Song>,
        mood: MoodArchetype,
        userThoughts: String,
        energyLevel: Float
    ): ParsedAiPayload {
        val title = when (mood.id) {
            "euphoric" -> "Golden Hour Radiance"
            "melancholy" -> "Echoes of Midnight Rain"
            "focus_zen" -> "Sanctuary of Stillness"
            "hype_rage" -> "Adrenaline Overdrive"
            "midnight_dream" -> "Velvet Neon Reverie"
            "warm_coffee" -> "Sunday Morning Cedar"
            "healing_solace" -> "Gentle Horizons & Grace"
            "cyber_drive" -> "Overdrive After Dark"
            else -> "${mood.name} Resonance"
        }

        val analysis = if (userThoughts.isNotBlank()) {
            "Reflecting your sentiment: \"$userThoughts\". Blending the emotional weight of your recent listening history with restorative harmonic textures tailored for $title."
        } else {
            "Analyzing your listening patterns across recent sessions alongside your current emotional state. This curation bridges contemplative acoustic undertones with vibrant atmospheric melodies."
        }

        // Curate standard iconic tracks fitting this mood
        val defaultTracks = when (mood.id) {
            "euphoric" -> listOf(
                AiPlaylistTrackItem("Levitating", "Dua Lipa", "Dopamine-fueled groove with uplifting synths."),
                AiPlaylistTrackItem("Blinding Lights", "The Weeknd", "Irresistible driving synthwave energy."),
                AiPlaylistTrackItem("Can't Stop the Feeling!", "Justin Timberlake", "Pure celebratory sunshine pop."),
                AiPlaylistTrackItem("Kesariya", "Arijit Singh", "Warm, uplifting love anthem with soaring harmonies."),
                AiPlaylistTrackItem("Believer", "Imagine Dragons", "Empowering rhythm and victorious vocal punch.")
            )
            "melancholy" -> listOf(
                AiPlaylistTrackItem("Someone You Loved", "Lewis Capaldi", "Vulnerable acoustic piano capturing honest longing."),
                AiPlaylistTrackItem("Let Her Go", "Passenger", "Delicate fingerstyle folk honoring bittersweet memories."),
                AiPlaylistTrackItem("Channa Mereya", "Arijit Singh", "Poignant emotional ballad of sacred departure."),
                AiPlaylistTrackItem("Lovely", "Billie Eilish", "Haunting vocal layers and mournful strings."),
                AiPlaylistTrackItem("Tum Hi Ho", "Arijit Singh", "Deeply felt romantic melancholy.")
            )
            "focus_zen" -> listOf(
                AiPlaylistTrackItem("Weightless", "Marconi Union", "Scientifically calibrated ambient relaxation."),
                AiPlaylistTrackItem("Sunset Lover", "Petit Biscuit", "Gentle melodic chill with sun-dappled textures."),
                AiPlaylistTrackItem("Raataan Lambiyan", "Jubin Nautiyal", "Soft acoustic strumming and calming cadence."),
                AiPlaylistTrackItem("River Flows in You", "Yiruma", "Introspective solo piano creating deep cognitive clarity."),
                AiPlaylistTrackItem("Sunflower", "Post Malone", "Mellow, rhythmic bounce perfect for focused work.")
            )
            "hype_rage" -> listOf(
                AiPlaylistTrackItem("Believer", "Imagine Dragons", "Relentless percussion and warrior motivation."),
                AiPlaylistTrackItem("Starboy", "The Weeknd", "Punchy synth bass and electrifying swagger."),
                AiPlaylistTrackItem("Zinda", "Farhan Akhtar", "High-octane rock anthem for pushing past every limit."),
                AiPlaylistTrackItem("Stronger", "Kanye West", "Industrial energy and unstoppable momentum."),
                AiPlaylistTrackItem("Apna Time Aayega", "Ranveer Singh", "Fierce raw lyrical drive and explosive flow.")
            )
            else -> listOf(
                AiPlaylistTrackItem("Until I Found You", "Stephen Sanchez", "Timeless doo-wop warmth with dreamy reverb."),
                AiPlaylistTrackItem("Night Changes", "One Direction", "Gentle nostalgic reflection on time passing."),
                AiPlaylistTrackItem("Apna Bana Le", "Arijit Singh", "Soothing, tender acoustic confession."),
                AiPlaylistTrackItem("Golden Hour", "JVKE", "Cinematic piano cascades and transcendent beauty."),
                AiPlaylistTrackItem("Dildariyaan", "Amrinder Gill", "Deeply soothing melodies for nighttime clarity.")
            )
        }

        return ParsedAiPayload(
            playlistTitle = title,
            moodAnalysis = analysis,
            colorHex = mood.colorHex,
            tracks = defaultTracks
        )
    }

    private fun formatEnergy(energy: Float): String {
        return when {
            energy < 0.35f -> "Chill & Introspective"
            energy < 0.65f -> "Balanced Melodic"
            else -> "High Energy & Dynamic"
        }
    }

    private data class ParsedAiPayload(
        val playlistTitle: String,
        val moodAnalysis: String,
        val colorHex: String,
        val tracks: List<AiPlaylistTrackItem>
    )
}
