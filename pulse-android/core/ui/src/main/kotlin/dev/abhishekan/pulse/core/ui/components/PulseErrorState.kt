package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString

@Composable
fun PulseErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        },
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(
            stringResource(R.string.pulse_ui_couldn_t_load_this_section),
            style = PulseTextStyle.Title,
        )
        PulseText(message)
        PulseTextButton(stringResource(R.string.pulse_ui_try_again), onRetry)
    }
}

@Composable
fun PulseErrorState(
    message: UiText,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) = PulseErrorState(message.asString(), onRetry, modifier)
