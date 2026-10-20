package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

enum class PulseButtonVariant { Primary, Secondary, Tertiary, Destructive }

@Composable
fun PulseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: PulseButtonVariant = PulseButtonVariant.Primary,
) {
    val colors = PulseTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val container =
        when (variant) {
            PulseButtonVariant.Primary -> if (pressed) colors.primaryPressed else colors.primary
            PulseButtonVariant.Secondary -> colors.surface
            PulseButtonVariant.Tertiary -> Color.Transparent
            PulseButtonVariant.Destructive -> colors.error
        }
    val foreground =
        when (variant) {
            PulseButtonVariant.Primary -> colors.onPrimary
            PulseButtonVariant.Destructive -> colors.background
            else -> colors.action
        }
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
        shape = PulseTheme.shapes.small,
        interactionSource = interaction,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = foreground,
                disabledContainerColor = colors.surfaceMuted,
                disabledContentColor = colors.textSecondary,
            ),
        border = if (variant == PulseButtonVariant.Secondary) BorderStroke(PulseTokens.focusWidth / 2, colors.outline) else null,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = PulseTheme.elevation.none,
                pressedElevation = PulseTheme.elevation.none,
                focusedElevation = PulseTheme.elevation.none,
                hoveredElevation = PulseTheme.elevation.none,
                disabledElevation = PulseTheme.elevation.none,
            ),
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
