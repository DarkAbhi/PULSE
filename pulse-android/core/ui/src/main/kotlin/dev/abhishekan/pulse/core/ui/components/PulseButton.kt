package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val loadingDescription = stringResource(R.string.pulse_ui_loading)
    Button(
        onClick,
        modifier.defaultMinSize(minHeight = PulseTokens.target).semantics {
            if (loading) {
                stateDescription =
                    loadingDescription
            }
        },
        enabled = enabled && !loading,
        shape = PulseTokens.controlShape,
    ) {
        if (loading) {
            CircularProgressIndicator(
                Modifier.size(PulseTokens.medium),
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(PulseTokens.small))
        }
        PulseText(text, style = PulseTextStyle.Label)
    }
}
