package dev.abhishekan.pulse.core.ui.patterns

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalResources
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.resolve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
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

@Composable
fun PulseSnackbarEffect(messages: Flow<UiText>) {
    val show = LocalPulseSnackbar.current
    val resources = rememberUpdatedState(LocalResources.current)
    LaunchedEffect(messages, show) {
        if (show != null) {
            messages.collectLatest { show(it.resolve(resources.value)) }
        }
    }
}
