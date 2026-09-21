package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SyncedLyricsView(
    lyricsData: LyricsData?,
    isLoading: Boolean,
    currentPosMs: Long,
    onSeekToTime: (Long) -> Unit,
    modifier: Modifier = Modifier,
    lyricsOffsetMs: Long = 0L,
    isKaraokeMode: Boolean = false,
    onAdjustOffset: (Long) -> Unit = {},
    onResetOffset: () -> Unit = {},
    onToggleKaraoke: () -> Unit = {},
    onRefresh: () -> Unit = {},
    trackName: String = "",
    artistName: String = ""
) {
    val clipboardManager = LocalClipboardManager.current
    var isOffsetBarVisible by remember { mutableStateOf(false) }
    var copiedFeedback by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("lyrics_loading_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Synchronizing live lyrics...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fetching real-time timestamps from LRCLIB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    if (lyricsData == null || (lyricsData.lines.isEmpty() && lyricsData.plainLyrics.isNullOrBlank())) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("lyrics_empty_view"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No lyrics found for this track",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We couldn't retrieve timestamped lyrics for '$trackName'. You can tap refresh to try searching again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Search Again",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        return
    }

    val lines = lyricsData.lines
    val effectivePos = currentPosMs + lyricsOffsetMs

    // Find active line index
    val activeIndex by remember(lines, effectivePos) {
        derivedStateOf {
            var index = -1
            for (i in lines.indices) {
                if (effectivePos >= lines[i].timeMs) {
                    index = i
                } else {
                    break
                }
            }
            index
        }
    }

    val activeLine = lines.getOrNull(activeIndex)

    // Calculate line progress for active line (0f to 1f)
    val activeLineProgress by remember(activeLine, effectivePos) {
        derivedStateOf {
            if (activeLine == null) 0f
            else {
                val duration = (activeLine.endTimeMs - activeLine.timeMs).coerceAtLeast(1000L)
                ((effectivePos - activeLine.timeMs).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val listState = rememberLazyListState()
    var userIsScrollingManually by remember { mutableStateOf(false) }

    // Detect if user is actively touching/scrolling
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userIsScrollingManually = true
        }
    }

    // Auto-scroll when active line changes (unless user paused or is browsing)
    LaunchedEffect(activeIndex, userIsScrollingManually) {
        if (!userIsScrollingManually && activeIndex in lines.indices) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("synced_lyrics_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar with status, calibration toggle, karaoke mode toggle, and copy
            LyricsControlHeader(
                isSynced = lyricsData.isSynced,
                source = lyricsData.source,
                lyricsOffsetMs = lyricsOffsetMs,
                isKaraokeMode = isKaraokeMode,
                isOffsetBarVisible = isOffsetBarVisible,
                onToggleOffsetBar = { isOffsetBarVisible = !isOffsetBarVisible },
                onToggleKaraoke = onToggleKaraoke,
                onRefresh = onRefresh,
                onCopyActiveLine = {
                    val textToCopy = activeLine?.text ?: lines.joinToString("\n") { it.text }
                    val attribution = if (trackName.isNotBlank()) "\n— '$trackName' by $artistName" else ""
                    clipboardManager.setText(AnnotatedString(textToCopy + attribution))
                    copiedFeedback = true
                    coroutineScope.launch {
                        delay(2000)
                        copiedFeedback = false
                    }
                },
                copiedFeedback = copiedFeedback
            )

            // Offset Calibration Bar (Slide in when opened)
            AnimatedVisibility(
                visible = isOffsetBarVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                LyricsOffsetToolbar(
                    offsetMs = lyricsOffsetMs,
                    onAdjust = onAdjustOffset,
                    onReset = onResetOffset
                )
            }

            // Main Lyrics Display: Karaoke Spotlight OR Full Scrollable Lyrics
            if (isKaraokeMode) {
                KaraokeSpotlightView(
                    lines = lines,
                    activeIndex = activeIndex,
                    activeLineProgress = activeLineProgress,
                    onSeekToTime = onSeekToTime,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 20.dp, end = 20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val isActive = index == activeIndex
                            val isPassed = index < activeIndex

                            LyricLineItem(
                                line = line,
                                isActive = isActive,
                                isPassed = isPassed,
                                lineProgress = if (isActive) activeLineProgress else 0f,
                                onLineClick = {
                                    userIsScrollingManually = false
                                    onSeekToTime(line.timeMs)
                                }
                            )
                        }
                    }

                    // Floating "Resume Live Sync" pill when user scrolled away
                    if (userIsScrollingManually && activeIndex != -1) {
                        Surface(
                            onClick = {
                                userIsScrollingManually = false
                                coroutineScope.launch {
                                    val target = (activeIndex - 2).coerceAtLeast(0)
                                    listState.animateScrollToItem(target)
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .testTag("resume_sync_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Resume Live Sync",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsControlHeader(
    isSynced: Boolean,
    source: String,
    lyricsOffsetMs: Long,
    isKaraokeMode: Boolean,
    isOffsetBarVisible: Boolean,
    onToggleOffsetBar: () -> Unit,
    onToggleKaraoke: () -> Unit,
    onRefresh: () -> Unit,
    onCopyActiveLine: () -> Unit,
    copiedFeedback: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source & Sync Status Pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSynced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isSynced) MaterialTheme.colorScheme.primary else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isSynced) "LIVE SYNCED" else "PLAIN TEXT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSynced) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (lyricsOffsetMs != 0L) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${if (lyricsOffsetMs > 0) "+" else ""}${lyricsOffsetMs}ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Action icons: Calibration, Karaoke Spotlight, Refresh, Copy
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Calibration / Latency Offset toggle
            IconButton(
                onClick = onToggleOffsetBar,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Calibrate sync latency",
                    tint = if (isOffsetBarVisible || lyricsOffsetMs != 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Karaoke Mode Toggle
            IconButton(
                onClick = onToggleKaraoke,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Toggle Karaoke spotlight mode",
                    tint = if (isKaraokeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Copy Active Quote
            IconButton(
                onClick = onCopyActiveLine,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy lyrics snippet",
                    tint = if (copiedFeedback) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Refresh / Reload lyrics
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload lyrics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsOffsetToolbar(
    offsetMs: Long,
    onAdjust: (Long) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sync Latency Calibration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Current: ${if (offsetMs > 0) "+" else ""}${offsetMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { onAdjust(-500L) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "-0.5s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    onClick = { onAdjust(-100L) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "-0.1s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    onClick = onReset,
                    shape = RoundedCornerShape(8.dp),
                    color = if (offsetMs == 0L) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Reset (0s)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (offsetMs == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    onClick = { onAdjust(100L) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "+0.1s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    onClick = { onAdjust(500L) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "+0.5s",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: LyricLine,
    isActive: Boolean,
    isPassed: Boolean,
    lineProgress: Float,
    onLineClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.03f else 1f,
        animationSpec = tween(250),
        label = "lyric_scale"
    )

    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> MaterialTheme.colorScheme.primary
            isPassed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        },
        animationSpec = tween(200),
        label = "lyric_color"
    )

    Surface(
        onClick = onLineClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(animatedScale)
            .testTag("lyric_line_${line.timeMs}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left active dot / indicator
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Text(
                    text = line.text,
                    style = if (isActive) {
                        MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            lineHeight = 30.sp
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = if (isPassed) FontWeight.Normal else FontWeight.Medium,
                            color = textColor,
                            lineHeight = 24.sp
                        )
                    },
                    modifier = Modifier.weight(1f)
                )

                // Timestamp pill (tap to seek)
                Text(
                    text = formatMillis(line.timeMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Real-time progressive line fill / progress bar when active
            if (isActive) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { lineProgress },
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
private fun KaraokeSpotlightView(
    lines: List<LyricLine>,
    activeIndex: Int,
    activeLineProgress: Float,
    onSeekToTime: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val prevLine = if (activeIndex > 0) lines.getOrNull(activeIndex - 1) else null
    val currentLine = lines.getOrNull(activeIndex)
    val nextLine = lines.getOrNull(activeIndex + 1)
    val afterNextLine = lines.getOrNull(activeIndex + 2)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Previous Line (faded above)
            if (prevLine != null) {
                Text(
                    text = prevLine.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clickable { onSeekToTime(prevLine.timeMs) }
                )
            }

            // Big Spotlight Current Line (Centerpiece)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentLine?.text ?: "♪ (Instrumental Break) ♪",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Progressive Karaoke Fill Bar
                    LinearProgressIndicator(
                        progress = { activeLineProgress },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentLine != null) {
                        Text(
                            text = formatMillis(currentLine.timeMs),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Next Upcoming Lines (Preview below)
            if (nextLine != null) {
                Text(
                    text = nextLine.text,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clickable { onSeekToTime(nextLine.timeMs) }
                )
            }

            if (afterNextLine != null) {
                Text(
                    text = afterNextLine.text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clickable { onSeekToTime(afterNextLine.timeMs) }
                )
            }
        }
    }
}
