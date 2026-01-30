package com.auracle.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auracle.R
import com.auracle.playback.PlaybackManager
import com.auracle.ui.components.AudiobookCover
import com.auracle.ui.components.ExpressiveProgressBar
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackManager: PlaybackManager,
    onBack: () -> Unit
) {
    val audiobook by playbackManager.currentAudiobook.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    
    var playbackSpeed by remember { mutableStateOf(1.0f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playbackManager.updateProgress()
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Now Playing", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            
            // Cover Art Card (entrance scale)
            var coverVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { coverVisible = true }
            val coverScale by animateFloatAsState(
                targetValue = if (coverVisible) 1f else 0.95f,
                animationSpec = tween(320),
                label = "coverScale"
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer(scaleX = coverScale, scaleY = coverScale)
                    .clip(RoundedCornerShape(48.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AudiobookCover(
                    coverArt = audiobook?.coverArt,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(48.dp),
                    fallbackIconSize = 80.dp
                )
                
                // Bookmark Icon on cover
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Info
            FadingMarqueeText(
                text = audiobook?.title ?: "Unknown Title",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth()
            )
            FadingMarqueeText(
                text = audiobook?.author ?: "Unknown Author",
                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(8.dp))
            
            FadingMarqueeText(
                text = "Narrated by ${audiobook?.author ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Tag/Chapter Info
            val currentChapter by playbackManager.currentChapter.collectAsState()
            val chapters by playbackManager.chapters.collectAsState()
            val currentChapterIndex by playbackManager.currentChapterIndex.collectAsState()
            var showChapterSheet by remember { mutableStateOf(false) }

            val currentChapterDuration = chapters.getOrNull(currentChapterIndex)?.duration ?: 0L
            Surface(
                onClick = { showChapterSheet = true },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${currentChapterIndex + 1}. ${currentChapter ?: "Chapter 1"} - ${formatDuration(currentChapterDuration)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp).basicMarquee(),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }

            if (showChapterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showChapterSheet = false },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "Chapters",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                        LazyColumn {
                            itemsIndexed(chapters) { index, chapter ->
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            chapter.name,
                                            fontWeight = if (index == currentChapterIndex) FontWeight.Bold else FontWeight.Normal,
                                            color = if (index == currentChapterIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ) 
                                    },
                                    supportingContent = { Text(formatDuration(chapter.duration)) },
                                    leadingContent = {
                                        if (index == currentChapterIndex) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        } else {
                                            Text("${index + 1}", modifier = Modifier.width(24.dp))
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        playbackManager.seekToChapter(index)
                                        showChapterSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            FadingMarqueeText(
                text = currentChapter ?: "Chapter 1",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Bar (clamp to 0..1 to avoid flash when opening or when reopening finished book)
            val progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
            ExpressiveProgressBar(
                progress = progress,
                onProgressChange = { playbackManager.seekTo(((it.coerceIn(0f, 1f)) * duration).toLong()) },
                isWaveActive = isPlaying
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDuration(currentPosition), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { playbackManager.skipBackward() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Replay10, contentDescription = "Back 10")
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                Surface(
                    onClick = { if (isPlaying) playbackManager.pause() else playbackManager.play() },
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.8f, animationSpec = tween(150)))
                                    .togetherWith(fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f, animationSpec = tween(150)))
                            },
                            label = "playPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                Surface(
                    onClick = { playbackManager.skipForward() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Bottom bar controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Surface(
                    onClick = { 
                        playbackSpeed = when (playbackSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        playbackManager.setPlaybackSpeed(playbackSpeed)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        "${playbackSpeed}x",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks")
                }
                
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.NightsStay, contentDescription = "Sleep Timer")
                }
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FadingMarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    fadeColor: Color = MaterialTheme.colorScheme.surface
) {
    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                // Paint background color over the edges so the text fades into the background (no dark band)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to fadeColor,
                        0.08f to fadeColor.copy(alpha = 0f),
                        0.5f to Color.Transparent,
                        0.92f to fadeColor.copy(alpha = 0f),
                        1f to fadeColor
                    )
                )
            }
    ) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    velocity = 30.dp,
                    spacing = androidx.compose.foundation.MarqueeSpacing.fractionOfContainer(1f/3f)
                )
        )
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
