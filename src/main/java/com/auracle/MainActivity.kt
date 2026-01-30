package com.auracle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.auracle.data.PreferenceManager
import com.auracle.ui.components.DraggablePlayerOverlay
import com.auracle.ui.screens.HomeScreen
import com.auracle.ui.screens.SetupScreen
import com.auracle.ui.screens.SplashScreen
import com.auracle.ui.screens.PlayerScreen
import com.auracle.ui.theme.AuracleTheme
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val preferenceManager = PreferenceManager(this)
        
        setContent {
            AuracleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var showPlayer by remember { mutableStateOf(false) }
                    val folderUriStr by preferenceManager.folderUri.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()
                    
                    val playbackViewModel: com.auracle.ui.viewmodel.PlaybackViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else if (folderUriStr == null) {
                        SetupScreen(onFolderSelected = { uri ->
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            scope.launch {
                                preferenceManager.setFolderUri(uri.toString())
                            }
                        })
                    } else {
                        val playerOffset = remember { Animatable(1f) }
                        LaunchedEffect(showPlayer) {
                            if (showPlayer) {
                                playerOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            } else {
                                playerOffset.snapTo(1f)
                            }
                        }
                        fun dismissPlayer() {
                            scope.launch {
                                playerOffset.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                showPlayer = false
                            }
                        }
                        Box(Modifier.fillMaxSize()) {
                            HomeScreen(
                                modifier = Modifier.alpha(0.4f + 0.6f * playerOffset.value),
                                folderUri = Uri.parse(folderUriStr),
                                onAudiobookClick = { book ->
                                    playbackViewModel.playAudiobook(book)
                                    showPlayer = true
                                },
                                onResumeClick = { showPlayer = true },
                                playbackViewModel = playbackViewModel
                            )
                            if (showPlayer) {
                                BackHandler { dismissPlayer() }
                                DraggablePlayerOverlay(
                                    offsetFraction = playerOffset.value,
                                    onDrag = { delta ->
                                        scope.launch {
                                            playerOffset.snapTo((playerOffset.value + delta * 1.4f).coerceIn(0f, 1f))
                                        }
                                    },
                                    onDragEnd = {
                                        scope.launch {
                                            if (playerOffset.value > 0.12f) {
                                                playerOffset.animateTo(1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                                showPlayer = false
                                            } else {
                                                playerOffset.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                            }
                                        }
                                    }
                                ) {
                                    PlayerScreen(
                                        playbackManager = playbackViewModel.playbackManager,
                                        onBack = { dismissPlayer() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
