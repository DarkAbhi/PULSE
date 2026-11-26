package dev.abhishekan.pulse.session

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.R
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState

@Composable
internal fun CheckingSession() {
    Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets)) { size ->
            PulseCenteredLoadingState(
                stringResource(R.string.pulse_app_checking_your_session),
                Modifier.fillMaxSize().padding(size.pagePadding),
            )
        }
    }
}

@Composable
internal fun SessionUnavailable(
    failure: Failure,
    busy: Boolean,
    onRetry: () -> Unit,
    onForget: () -> Unit,
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    Scaffold(containerColor = PulseTheme.colors.background, contentColor = PulseTheme.colors.textPrimary) { insets ->
        PulseAdaptiveLayout(Modifier.fillMaxSize().padding(insets)) { size ->
            Box(Modifier.fillMaxSize().padding(size.pagePadding), contentAlignment = Alignment.Center) {
                PulseCard(
                    Modifier
                        .widthIn(
                            max = PulseTokens.readingWidth,
                        ).fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    PulseText(
                        stringResource(R.string.pulse_app_we_couldn_t_open_your_session),
                        style = PulseTextStyle.Title,
                    )
                    PulseText(
                        stringResource(
                            R.string.pulse_app_we_couldn_t_validate_your_session_1_s,
                            failure.message.asString(),
                        ),
                    )
                    PulseText(
                        stringResource(
                            R.string.pulse_app_your_saved_session_has_been_kept_on_this_device,
                        ),
                        color = PulseTheme.colors.textSecondary,
                    )
                    PulseButton(stringResource(R.string.pulse_app_try_again), onRetry, enabled = !busy)
                    PulseButton(
                        stringResource(R.string.pulse_app_forget_this_device_s_session),
                        onClick = { confirm = true },
                        enabled = !busy,
                        style = PulseButtonStyle.Text,
                    )
                }
            }
        }
    }
    if (confirm) {
        PulseDialog(
            stringResource(R.string.pulse_app_forget_this_session),
            stringResource(R.string.pulse_app_this_removes_this_device_s_saved_session_it_won),
            stringResource(R.string.pulse_app_forget_session),
            {
                confirm = false
                onForget()
            },
            { confirm = false },
        )
    }
}
