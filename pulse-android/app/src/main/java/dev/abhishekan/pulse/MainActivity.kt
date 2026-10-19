package dev.abhishekan.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseDarkColors
import dev.abhishekan.pulse.core.ui.PulseLightColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: AppearanceViewModel = koinViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dark =
                when (state.theme) {
                    ThemePreference.System -> isSystemInDarkTheme()
                    ThemePreference.Light -> false
                    ThemePreference.Dark -> true
                }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle =
                        SystemBarStyle.auto(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        ) {
                            dark
                        },
                    navigationBarStyle =
                        SystemBarStyle.auto(
                            PulseLightColors.background.toArgb(),
                            PulseDarkColors.background.toArgb(),
                        ) {
                            dark
                        },
                )
                onDispose { }
            }
            PulseTheme(dark) {
                LocalNetworkAccess { PulseApp(state, viewModel::selectTheme) }
            }
        }
    }
}
