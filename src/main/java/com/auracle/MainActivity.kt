package com.auracle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.auracle.data.PreferenceManager
import com.auracle.ui.screens.HomeScreen
import com.auracle.ui.screens.SetupScreen
import com.auracle.ui.screens.SplashScreen
import com.auracle.ui.theme.AuracleTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        val preferenceManager = PreferenceManager(this)
        
        setContent {
            AuracleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    val folderUriStr by preferenceManager.folderUri.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()
                    
                    if (showSplash) {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    } else {
                        if (folderUriStr == null) {
                            SetupScreen(onFolderSelected = { uri ->
                                // Take persistable permission
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                                // Save to datastore
                                scope.launch {
                                    preferenceManager.setFolderUri(uri.toString())
                                }
                            })
                        } else {
                            HomeScreen(folderUri = Uri.parse(folderUriStr))
                        }
                    }
                }
            }
        }
    }
}
