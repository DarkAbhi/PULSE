package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    TextButton(
        onClick,
        modifier.defaultMinSize(minHeight = PulseTokens.target),
        enabled = enabled,
        shape = PulseTheme.shapes.small,
        colors =
            ButtonDefaults.textButtonColors(
                contentColor = if (color == Color.Unspecified) PulseTheme.colors.action else color,
                disabledContentColor = PulseTheme.colors.textSecondary,
            ),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(PulseTokens.small))
        }
        PulseText(text, style = PulseTextStyle.Label)
    }
}
