package com.scorigami.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.scorigami.app.navigation.AppNavigation
import com.scorigami.app.ui.theme.ScorigamiTheme
import com.scorigami.shared.sync.SgCourse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pending course import from an ACTION_VIEW intent. Consumed once by AppNavigation. */
    val pendingImport = mutableStateOf<SgCourse?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)
        setContent {
            ScorigamiTheme {
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

