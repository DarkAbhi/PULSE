package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseIconButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick,
        modifier.sizeIn(minWidth = PulseTokens.target, minHeight = PulseTokens.target).semantics {
            contentDescription =
                label
        },
        enabled = enabled,
    ) { icon() }
}
