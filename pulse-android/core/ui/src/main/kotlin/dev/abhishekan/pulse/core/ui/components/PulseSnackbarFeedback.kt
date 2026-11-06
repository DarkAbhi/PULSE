package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import kotlinx.coroutines.launch

private val LocalPulseSnackbar = staticCompositionLocalOf<((String) -> Unit)?> { null }

/** Routes share the scaffold host, which positions feedback above navigation. */
@Composable
fun PulseSnackbarProvider(
    host: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val show =
        remember(host, scope) {
            { message: String ->
                scope.launch {
                    host.showSnackbar(message, withDismissAction = true, duration = SnackbarDuration.Short)
                }
                Unit
            }
        }
    CompositionLocalProvider(LocalPulseSnackbar provides show, content = content)
}

/** Consume transient feedback on enqueue so it cannot replay on returning to a route. */
@Composable
fun PulseSnackbarEffect(
    message: UiText?,
    onConsumed: () -> Unit,
) {
    val show = LocalPulseSnackbar.current
    val text = message?.asString()
    LaunchedEffect(message, show) {
        if (text != null && show != null) {
            show(text)
            onConsumed()
        }
    }
}
