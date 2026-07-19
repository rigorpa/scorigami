package com.scorigami.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.scorigami.app.viewmodel.SettingsViewModel
import com.scorigami.app.navigation.AppNavigation
import com.scorigami.app.ui.theme.ScorigamiTheme
import com.scorigami.shared.sync.SgCourse
import com.scorigami.shared.sync.SgHistory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Pending course import from an ACTION_VIEW intent. Consumed once by AppNavigation. */
    val pendingImport = mutableStateOf<SgCourse?>(null)

    /** Pending round-history import from an ACTION_VIEW intent. Consumed once by HistoryScreen. */
    val pendingHistoryImport = mutableStateOf<SgHistory?>(null)

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)
        setContent {
            val fontSize by settingsViewModel.fontSize.collectAsStateWithLifecycle()
            ScorigamiTheme(fontSize = fontSize) {
                AppNavigation(
                    pendingImport = pendingImport,
                    pendingHistoryImport = pendingHistoryImport
                )
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
                val json = readSgFile(uri) ?: return@launch
                // Both formats arrive as application/octet-stream; discriminate by shape.
                // SgHistory has a required `rounds` array, SgCourse a required `name` —
                // decoding the wrong type fails, so try history first, then course.
                val history = runCatching { Json.decodeFromString<SgHistory>(json) }.getOrNull()
                if (history != null) {
                    withContext(Dispatchers.Main) { pendingHistoryImport.value = history }
                    return@launch
                }
                val course = runCatching { Json.decodeFromString<SgCourse>(json) }.getOrNull()
                if (course != null) {
                    withContext(Dispatchers.Main) { pendingImport.value = course }
                } else {
                    Log.e("MainActivity", "File is neither valid .sghistory nor .sgcourse JSON")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to read shared file", e)
            }
        }
    }

    private fun readSgFile(uri: Uri): String? {
        // Reject oversized files — a valid .sgcourse is a few KB; even a large .sghistory
        // stays well under this.
        val size = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
        if (size != null && size > 10_000_000L) {
            Log.w("MainActivity", "Skipping oversized file: $size bytes")
            return null
        }
        return contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    }
}
