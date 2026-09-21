package com.example.data.model

data class MoodArchetype(
    val id: String,
    val name: String,
    val emoji: String,
    val tagline: String,
    val colorHex: String,
    val defaultEnergy: Float = 0.5f
)

data class AiPlaylistTrackItem(
    val title: String,
    val artist: String,
    val emotionalReason: String
)

data class AiPlaylistResult(
    val title: String,
    val moodAnalysis: String,
    val primaryMood: String,
    val energyRating: String,
    val colorHex: String,
    val songs: List<Song>,
    val songReasons: Map<String, String> = emptyMap(),
    val generatedAt: Long = System.currentTimeMillis()
)

sealed interface AiGenerationUiState {
    object Idle : AiGenerationUiState
    data class Generating(val stepMessage: String) : AiGenerationUiState
    data class Success(val result: AiPlaylistResult) : AiGenerationUiState
    data class Error(val message: String) : AiGenerationUiState
}
