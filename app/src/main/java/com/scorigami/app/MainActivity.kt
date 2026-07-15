package com.scorigami.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.scorigami.app.data.ThemeRepository
import com.scorigami.app.navigation.AppNavigation
import com.scorigami.app.ui.theme.ScorigamiTheme
import com.scorigami.shared.sync.SgCourse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pending course import from an ACTION_VIEW intent. Consumed once by AppNavigation. */
    val pendingImport = mutableStateOf<SgCourse?>(null)

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Blocking one-boolean read so the first frame renders in the right theme
        val initialDark = themeRepository.isDarkBlocking()
        handleIncomingIntent(intent)
        setContent {
            val isDark by themeRepository.isDark.collectAsStateWithLifecycle(initialValue = initialDark)
            // Re-apply edge-to-edge on theme change so status/nav bar icon contrast flips
            LaunchedEffect(isDark) {
                val transparent = android.graphics.Color.TRANSPARENT
                if (isDark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(transparent),
                        navigationBarStyle = SystemBarStyle.dark(transparent)
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(transparent, transparent),
                        navigationBarStyle = SystemBarStyle.light(transparent, transparent)
                    )
                }
            }
            ScorigamiTheme(darkTheme = isDark) {
                AppNavigation(pendingImport = pendingImport)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sgCourse = readSgCourse(uri)
                if (sgCourse != null) {
                    withContext(Dispatchers.Main) { pendingImport.value = sgCourse }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to read .sgcourse file", e)
            }
        }
    }

    private fun readSgCourse(uri: Uri): SgCourse? {
        // Reject files larger than 1 MB — a valid .sgcourse is a few KB at most.
        val size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        if (size != null && size > 1_000_000L) {
            Log.w("MainActivity", "Skipping oversized file: $size bytes")
            return null
        }
        val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return null
        return try {
            Json.decodeFromString<SgCourse>(json)
        } catch (e: Exception) {
            Log.e("MainActivity", "Invalid .sgcourse JSON", e)
            null
        }
    }
}

