package com.scanflow.photocompressor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.scanflow.photocompressor.ui.PhotoCompressorApp
import com.scanflow.photocompressor.ui.theme.PhotoCompressorTheme
import com.scanflow.photocompressor.util.ShareIntentHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.scanflow.photocompressor.domain.model.AppPreferences
import com.scanflow.photocompressor.domain.model.ThemeMode
import com.scanflow.photocompressor.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity entry point for the application.
 * Receives external share intents (ACTION_SEND and ACTION_SEND_MULTIPLE).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    private val incomingSharedUris = mutableStateOf<List<Uri>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val preferences by preferencesRepository.preferencesFlow.collectAsState(initial = AppPreferences())
            val isDarkTheme = when (preferences.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            PhotoCompressorTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PhotoCompressorApp(
                        incomingSharedUris = incomingSharedUris.value,
                        onSharedUrisHandled = { incomingSharedUris.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uris = ShareIntentHandler.extractUris(intent)
        if (uris.isNotEmpty()) {
            incomingSharedUris.value = uris
        }
    }
}
