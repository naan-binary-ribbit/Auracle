package com.auracle.ui.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.auracle.data.Audiobook
import com.auracle.data.AudiobookScanner
import com.auracle.ui.components.AnimatedEqualizerBars
import com.auracle.ui.components.AudiobookCover
import com.auracle.ui.components.ExpressiveProgressBar
import com.auracle.ui.theme.MomoSignature
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.auracle.ui.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    folderUri: Uri,
    onAudiobookClick: (Audiobook) -> Unit,
    onResumeClick: () -> Unit,
    playbackViewModel: com.auracle.ui.viewmodel.PlaybackViewModel,
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val audiobooks by homeViewModel.audiobooks.collectAsState()
    val isLoading by homeViewModel.isLoading.collectAsState()
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    
    val currentAudiobook by playbackViewModel.playbackManager.currentAudiobook.collectAsState()
    val isPlaying by playbackViewModel.playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackViewModel.playbackManager.currentPosition.collectAsState()
    val duration by playbackViewModel.playbackManager.duration.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(folderUri) {
        homeViewModel.loadAudiobooks(folderUri)
    }

    var hasRestoredPlayback by remember { mutableStateOf(false) }
    LaunchedEffect(audiobooks) {
        if (audiobooks.isNotEmpty() && !hasRestoredPlayback) {
            playbackViewModel.resumeLastPlayed(audiobooks)
            hasRestoredPlayback = true
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                playbackViewModel.playbackManager.updateProgress()
                delay(1000)
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            homeViewModel.loadAudiobooks(folderUri, forceRefresh = true)
        }
    }

    val gridState = rememberLazyGridState()
    val scrolled = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
    val topBarAlpha by animateFloatAsState(
        targetValue = if (scrolled) 0.92f else 1f,
        animationSpec = tween(200),
        label = "topBarAlpha"
    )
    val blurRadius by animateFloatAsState(
        targetValue = if (scrolled) 8f else 0f,
        animationSpec = tween(200),
        label = "blurRadius"
    )
    val topBarColor = MaterialTheme.colorScheme.surface

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Only the background becomes transparent and blurred; content stays sharp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .blur(radius = blurRadius.toInt().dp)
                        .background(topBarColor.copy(alpha = topBarAlpha))
                )
                TopAppBar(
                    title = {
                        Text(
                            text = "Auracle",
                            fontFamily = MomoSignature,
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { /* TODO */ },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = currentAudiobook != null,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.9f, animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.9f, animationSpec = tween(180))
            ) {
                // Mini Player / Resume Button
                Surface(
                    onClick = onResumeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Book Cover
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            AudiobookCover(
                                coverArt = currentAudiobook?.coverArt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                fallbackIconSize = 24.dp
                            )
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = currentAudiobook?.title ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentAudiobook?.author ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        
                        IconButton(onClick = { if (isPlaying) playbackViewModel.playbackManager.pause() else playbackViewModel.playbackManager.play() }) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        IconButton(onClick = { playbackViewModel.playbackManager.skipForward() }) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = null)
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            if (isLoading && audiobooks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding() + 100.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "My Collection",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 32.sp
                            )
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = "LISTENING NOW",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(12.dp))
                            ListeningNowCard(
                                book = currentAudiobook ?: audiobooks.firstOrNull(),
                                isPlaying = currentAudiobook != null && isPlaying,
                                progress = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                                onClick = { (currentAudiobook ?: audiobooks.firstOrNull())?.let { onAudiobookClick(it) } },
                                onProgressChange = { p -> if (duration > 0) playbackViewModel.playbackManager.seekTo((p.coerceIn(0f, 1f) * duration).toLong()) }
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 20.dp)
                        ) {
                            item { FilterChip(selected = true, text = "All Books") }
                        }
                    }

                    itemsIndexed(audiobooks) { index, book ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(index * 40L)
                            visible = true
                        }
                        val scale by animateFloatAsState(
                            targetValue = if (visible) 1f else 0.92f,
                            animationSpec = tween(280),
                            label = "gridItemScale"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(220),
                            label = "gridItemAlpha"
                        )
                        val progressState by homeViewModel.getBookProgress(book.id).collectAsState(initial = Pair(0, 0L))
                        val totalDuration = book.duration
                        val currentPlayed = if (book.audioFiles.isNotEmpty()) {
                             book.audioFiles.take(progressState.first).sumOf { it.duration } + progressState.second
                        } else 0L
                        val progress = if (totalDuration > 0) (currentPlayed.toFloat() / totalDuration).coerceIn(0f, 1f) else 0f
                        val isFinished = totalDuration > 0 && currentPlayed >= totalDuration

                        Box(
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    alpha = alpha
                                )
                        ) {
                            AudiobookGridItem(
                                book = book,
                                progress = progress,
                                isFinished = isFinished,
                                onClick = { onAudiobookClick(book) }
                            )
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}


@Composable
fun ListeningNowCard(
    book: Audiobook?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onProgressChange: (Float) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = book != null) { onClick() },
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                AudiobookCover(
                    coverArt = book?.coverArt,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    shape = RoundedCornerShape(12.dp),
                    fallbackIconSize = 40.dp
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = book?.title ?: "No book playing",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book?.author ?: "Unknown Author",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isPlaying) {
                        AnimatedEqualizerBars(
                            modifier = Modifier.size(width = 32.dp, height = 24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            barWidth = 3.dp,
                            maxBarHeight = 20.dp,
                            minBarHeight = 4.dp
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveProgressBar(
                        progress = progress,
                        onProgressChange = onProgressChange,
                        modifier = Modifier.weight(1f),
                        height = 8.dp,
                        isWaveActive = isPlaying
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, text: String) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = CircleShape,
        modifier = Modifier.clickable { /* TODO */ }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun AudiobookGridItem(book: Audiobook, progress: Float, isFinished: Boolean = false, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(80),
        label = "pressScale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer(scaleX = pressScale, scaleY = pressScale)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AudiobookCover(
                coverArt = book.coverArt,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                shape = RoundedCornerShape(32.dp),
                fallbackIconSize = 64.dp
            )
            
            if (book.isNew) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = CircleShape
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (isFinished) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Finished",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }

            // Progress bar at bottom of cover
            if (progress > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = book.author ?: "Unknown Author",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    }
}
