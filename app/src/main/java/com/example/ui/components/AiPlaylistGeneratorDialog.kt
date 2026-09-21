package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.AiGenerationUiState
import com.example.data.model.AiPlaylistResult
import com.example.data.model.MoodArchetype
import com.example.data.model.Song
import com.example.data.remote.AiPlaylistService

@Composable
fun AiPlaylistGeneratorDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    uiState: AiGenerationUiState,
    selectedMood: MoodArchetype,
    customPrompt: String,
    energyLevel: Float,
    historyInfluence: Float,
    recentlyPlayed: List<Song>,
    likedSongs: List<Song>,
    onSelectMood: (MoodArchetype) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onEnergyLevelChange: (Float) -> Unit,
    onHistoryInfluenceChange: (Float) -> Unit,
    onGenerateClick: () -> Unit,
    onPlayPlaylist: (AiPlaylistResult) -> Unit,
    onSaveToLibrary: (AiPlaylistResult) -> Unit,
    onPlayTrack: (Song, List<Song>) -> Unit,
    onLikeToggle: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag("ai_playlist_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            when (uiState) {
                is AiGenerationUiState.Success -> {
                    AiPlaylistResultView(
                        result = uiState.result,
                        onBack = onDismiss,
                        onPlayPlaylist = { onPlayPlaylist(uiState.result) },
                        onSaveToLibrary = { onSaveToLibrary(uiState.result) },
                        onPlayTrack = { song -> onPlayTrack(song, uiState.result.songs) },
                        onLikeToggle = onLikeToggle,
                        onRegenerate = onGenerateClick
                    )
                }
                is AiGenerationUiState.Generating -> {
                    AiGeneratingLoadingView(
                        stepMessage = uiState.stepMessage,
                        mood = selectedMood
                    )
                }
                else -> {
                    AiPlaylistConfiguratorView(
                        selectedMood = selectedMood,
                        customPrompt = customPrompt,
                        energyLevel = energyLevel,
                        historyInfluence = historyInfluence,
                        recentlyPlayed = recentlyPlayed,
                        likedSongs = likedSongs,
                        errorMessage = (uiState as? AiGenerationUiState.Error)?.message,
                        onClose = onDismiss,
                        onSelectMood = onSelectMood,
                        onCustomPromptChange = onCustomPromptChange,
                        onEnergyLevelChange = onEnergyLevelChange,
                        onHistoryInfluenceChange = onHistoryInfluenceChange,
                        onGenerateClick = onGenerateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AiPlaylistConfiguratorView(
    selectedMood: MoodArchetype,
    customPrompt: String,
    energyLevel: Float,
    historyInfluence: Float,
    recentlyPlayed: List<Song>,
    likedSongs: List<Song>,
    errorMessage: String?,
    onClose: () -> Unit,
    onSelectMood: (MoodArchetype) -> Unit,
    onCustomPromptChange: (String) -> Unit,
    onEnergyLevelChange: (Float) -> Unit,
    onHistoryInfluenceChange: (Float) -> Unit,
    onGenerateClick: () -> Unit
) {
    val moodColor = parseHexColor(selectedMood.colorHex, MaterialTheme.colorScheme.primary)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        // Top App Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = moodColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shivam AI Mood Generator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Header Banner with gradient
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                moodColor.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = moodColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedMood.emoji,
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedMood.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedMood.tagline,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // User Listening Context Pill
                    val topArtists = (recentlyPlayed + likedSongs)
                        .map { it.artist.split(",").first().trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(3)
                        .joinToString(", ")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = moodColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (topArtists.isNotBlank()) {
                                "Listening Profile: $topArtists • ${recentlyPlayed.size} tracks active"
                            } else {
                                "Listening Profile: Fresh listener • Ready to curate"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section 1: Choose Emotional Mood Archetype
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "1. Choose Emotional Archetype",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AiPlaylistService.CURATED_MOODS) { mood ->
                        val isSelected = mood.id == selectedMood.id
                        val itemColor = parseHexColor(mood.colorHex, MaterialTheme.colorScheme.primary)

                        Surface(
                            onClick = { onSelectMood(mood) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) itemColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            tonalElevation = if (isSelected) 4.dp else 1.dp,
                            modifier = Modifier
                                .width(135.dp)
                                .height(115.dp)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) itemColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = mood.emoji,
                                    fontSize = 26.sp
                                )
                                Column {
                                    Text(
                                        text = mood.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) itemColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Freeform Emotional Thoughts / Reflections
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = moodColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "2. Express Your Thoughts (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Describe your mood, scene, or vibe in your own words. Shivam AI will analyze the emotional nuances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = onCustomPromptChange,
                    placeholder = {
                        Text(
                            text = "e.g. Staring at raindrops on my bedroom window, missing someone, need quiet acoustic solace...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_mood_input"),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = moodColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Quick Prompt Suggestion Pills
                Spacer(modifier = Modifier.height(10.dp))
                val quickPills = listOf(
                    "🌧️ Rain on windows",
                    "🔥 Gym PR hype",
                    "🌙 Late night drive",
                    "💔 Healing after heartbreak",
                    "☕ Sunday acoustic coffee",
                    "🧘 Focus study flow"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPills) { pill ->
                        Surface(
                            onClick = {
                                val clean = pill.substringAfter(" ").trim()
                                val newText = if (customPrompt.isBlank()) clean else "$customPrompt, $clean"
                                onCustomPromptChange(newText)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = pill,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Fine-Tune Energy & History Affinity Sliders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = moodColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. Mood & History Sliders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Energy Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Energy Dynamics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            energyLevel < 0.35f -> "Chill & Introspective (${(energyLevel * 100).toInt()}%)"
                            energyLevel < 0.65f -> "Moderate & Balanced (${(energyLevel * 100).toInt()}%)"
                            else -> "Intense & High Hype (${(energyLevel * 100).toInt()}%)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = moodColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = energyLevel,
                    onValueChange = onEnergyLevelChange,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = moodColor,
                        activeTrackColor = moodColor
                    ),
                    modifier = Modifier.testTag("slider_energy")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // History Affinity Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Listening History Weight",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            historyInfluence < 0.35f -> "Fresh Discoveries"
                            historyInfluence < 0.7f -> "Balanced Hybrid"
                            else -> "Deep Taste Affinity"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = moodColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = historyInfluence,
                    onValueChange = onHistoryInfluenceChange,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = moodColor,
                        activeTrackColor = moodColor
                    ),
                    modifier = Modifier.testTag("slider_history")
                )
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        }

        // Primary Action: Generate Button
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Button(
                    onClick = onGenerateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_generate_ai_playlist"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = moodColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Generate AI Mood Playlist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AiGeneratingLoadingView(
    stepMessage: String,
    mood: MoodArchetype
) {
    val moodColor = parseHexColor(mood.colorHex, MaterialTheme.colorScheme.primary)
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
            ) {
                // Outer glowing aura
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(moodColor.copy(alpha = 0.15f))
                )

                // Rotating gradient border
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .rotate(rotation)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(moodColor, Color.White, moodColor.copy(alpha = 0.2f), moodColor)
                            ),
                            shape = CircleShape
                        )
                )

                Text(
                    text = mood.emoji,
                    fontSize = 44.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Shivam AI Synthesizer",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stepMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = moodColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun AiPlaylistResultView(
    result: AiPlaylistResult,
    onBack: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onSaveToLibrary: () -> Unit,
    onPlayTrack: (Song) -> Unit,
    onLikeToggle: (Song) -> Unit,
    onRegenerate: () -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }
    val accentColor = parseHexColor(result.colorHex, MaterialTheme.colorScheme.primary)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // Top Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "AI Curated Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = onRegenerate) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = accentColor
                    )
                }
            }
        }

        // Hero Cover & Title Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        color = accentColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accentColor.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = result.primaryMood,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${result.energyRating}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "${result.songs.size} handpicked tracks for your emotional journey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sparkle Badge
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Emotional Mood Analysis Quote Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Shivam AI Mood Analysis",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${result.moodAnalysis}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPlayPlaylist,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_play_ai_playlist"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Playlist", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isSaved) {
                                    onSaveToLibrary()
                                    isSaved = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_save_ai_playlist"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSaved) accentColor else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSaved) "Saved" else "Save to Library",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Curated Tracks
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracklist with Mood Rationale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${result.songs.size} songs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Track items with emotional explanation
        items(result.songs) { song ->
            val reason = result.songReasons[song.id]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clickable { onPlayTrack(song) }
                    .testTag("ai_track_${song.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Artwork
                        AsyncImage(
                            model = song.art,
                            contentDescription = song.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play preview button
                        IconButton(
                            onClick = { onPlayTrack(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Like button
                        IconButton(
                            onClick = { onLikeToggle(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (song.isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Emotional Reason Tag
                    if (!reason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontStyle = FontStyle.Italic,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> "FF$clean".toLong(16).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return fallback
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}
