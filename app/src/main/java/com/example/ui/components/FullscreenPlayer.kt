package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lyrics
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.JamSession
import com.example.data.model.LyricsData
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.util.ShareHelper

@Composable
fun FullscreenPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: RepeatMode,
    playbackSpeed: Float,
    isLyricsViewActive: Boolean,
    lyricsData: LyricsData?,
    isLoadingLyrics: Boolean,
    jamSession: JamSession?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onToggleLyrics: () -> Unit,
    onLikeToggle: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenJam: () -> Unit,
    modifier: Modifier = Modifier,
    onShareTrack: ((Song) -> Unit)? = null,
    lyricsOffsetMs: Long = 0L,
    isKaraokeMode: Boolean = false,
    onAdjustLyricsOffset: (Long) -> Unit = {},
    onResetLyricsOffset: () -> Unit = {},
    onToggleKaraokeMode: () -> Unit = {},
    onRefreshLyrics: () -> Unit = {},
    isCrossfading: Boolean = false,
    crossfadeDurationMs: Int = 4000,
    onCrossfadeDurationChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val handleShare = {
        if (onShareTrack != null) {
            onShareTrack(song)
        } else {
            ShareHelper.shareTrack(context, song)
        }
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragValue by remember { mutableFloatStateOf(0f) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }

    val effectivePos = if (isDraggingSlider) {
        (sliderDragValue * durationMs).toLong()
    } else {
        currentPosMs
    }

    val sliderProgress = if (durationMs > 0) {
        (effectivePos.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedPrimaryColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "ambient_bg_crossfade"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("fullscreen_player"),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            animatedPrimaryColor.copy(alpha = 0.22f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("fullscreen_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse Player",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM SHIVAM MUSIC",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (jamSession != null) {
                            Surface(
                                onClick = onOpenJam,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Jam: ${jamSession.code}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = handleShare,
                            modifier = Modifier.testTag("fullscreen_share_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleLyrics,
                            modifier = Modifier.testTag("toggle_lyrics_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lyrics,
                                contentDescription = "Toggle Lyrics",
                                tint = if (isLyricsViewActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Middle Content: Artwork OR Lyrics
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLyricsViewActive) {
                        SyncedLyricsView(
                            lyricsData = lyricsData,
                            isLoading = isLoadingLyrics,
                            currentPosMs = effectivePos,
                            onSeekToTime = onSeekTo,
                            lyricsOffsetMs = lyricsOffsetMs,
                            isKaraokeMode = isKaraokeMode,
                            onAdjustOffset = onAdjustLyricsOffset,
                            onResetOffset = onResetLyricsOffset,
                            onToggleKaraoke = onToggleKaraokeMode,
                            onRefresh = onRefreshLyrics,
                            trackName = song.name,
                            artistName = song.artist
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // High Quality Album Cover with Smooth Crossfade Animation
                            Crossfade(
                                targetState = song,
                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                                label = "fullscreen_album_art_crossfade"
                            ) { currentSong ->
                                AsyncImage(
                                    model = currentSong.art.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600" },
                                    contentDescription = currentSong.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .aspectRatio(1f)
                                        .shadow(24.dp, RoundedCornerShape(24.dp))
                                        .clip(RoundedCornerShape(24.dp))
                                )
                            }

                            // Active Cross-fading indicator banner
                            AnimatedVisibility(
                                visible = isCrossfading,
                                enter = fadeIn(tween(300)) + expandVertically(),
                                exit = fadeOut(tween(300)) + shrinkVertically()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .testTag("crossfade_active_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Cross-fading with next track...",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Real-time Mini Lyrics Snippet Pill under Album Art
                            val currentLyricLine = lyricsData?.lines?.findLast { effectivePos + lyricsOffsetMs >= it.timeMs }
                            if (currentLyricLine != null) {
                                Surface(
                                    onClick = onToggleLyrics,
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth(0.85f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Lyrics,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = currentLyricLine.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "VIEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Song Title & Artist & Action Icons with Smooth Animated Crossfade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AnimatedContent(
                        targetState = song,
                        transitionSpec = {
                            fadeIn(tween(400, delayMillis = 60)) togetherWith fadeOut(tween(250))
                        },
                        modifier = Modifier.weight(1f),
                        label = "fullscreen_title_crossfade"
                    ) { currentSong ->
                        Column {
                            Text(
                                text = currentSong.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = handleShare,
                            modifier = Modifier.testTag("player_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Track",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = onAddToPlaylist) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = "Add to playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = onLikeToggle) {
                            Icon(
                                imageVector = if (song.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (song.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Slider & Timestamps
                Slider(
                    value = sliderProgress,
                    onValueChange = {
                        isDraggingSlider = true
                        sliderDragValue = it
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        onSeekTo((sliderDragValue * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMillis(effectivePos),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatMillis(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Controls Row: Shuffle, Previous, Big Play/Pause, Next, Repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Big Play/Pause Button
                    Surface(
                        onClick = onTogglePlayPause,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .testTag("fullscreen_play_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Repeat
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom bar: Playback Speed selector, Crossfade chip & High Quality Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback speed chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
                            val isSelected = (playbackSpeed == speed)
                            Surface(
                                onClick = { onSpeedChange(speed) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(28.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 7.dp)
                                ) {
                                    Text(
                                        text = "${speed}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Crossfade Duration Settings Button
                        Surface(
                            onClick = { showCrossfadeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (crossfadeDurationMs > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (crossfadeDurationMs > 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("crossfade_settings_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Crossfade",
                                    tint = if (crossfadeDurationMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (crossfadeDurationMs == 0) "Fade: Off" else "Fade: ${crossfadeDurationMs / 1000}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (crossfadeDurationMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Audio Quality Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = "HQ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Crossfade Duration Selector Dialog
    if (showCrossfadeDialog) {
        AlertDialog(
            onDismissRequest = { showCrossfadeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crossfade Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Smoothly transitions audio and visuals between tracks for an uninterrupted listening experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val presets = listOf(
                        0 to "Off (Direct cut)",
                        2000 to "2 seconds (Snappy)",
                        4000 to "4 seconds (Recommended)",
                        6000 to "6 seconds (Smooth blend)",
                        8000 to "8 seconds (Extended flow)",
                        12000 to "12 seconds (Club mix)"
                    )
                    presets.forEach { (duration, label) ->
                        val isSelected = (crossfadeDurationMs == duration)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onCrossfadeDurationChange(duration)
                                    showCrossfadeDialog = false
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onCrossfadeDurationChange(duration)
                                    showCrossfadeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrossfadeDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

fun formatMillis(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format("%d:%02d", m, s)
}
