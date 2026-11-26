package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

/**
 * Mirrors the web `PulseButton` variants: Primary (gradient fill in light mode), Secondary
 * (muted fill), Outline (divider border), Tertiary (web `ghost`), Destructive, plus the
 * Android-only tonal Soft and DestructiveSoft fills.
 */
enum class PulseButtonVariant { Primary, Secondary, Outline, Tertiary, Destructive, Soft, DestructiveSoft }

/** Presentation is independent of the action's visual emphasis ([PulseButtonVariant]). */
enum class PulseButtonStyle { Standard, Text, Icon }

/**
 * Shared action component. [text] is the accessibility label for [PulseButtonStyle.Icon].
 * Text and icon styles retain their native padding, shape, feedback, and disabled colors.
 * Supply [icon] for icon-only actions, or [leadingIcon] alongside visible text.
 */
@Composable
fun PulseButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    variant: PulseButtonVariant = PulseButtonVariant.Primary,
    leadingIcon: (@Composable () -> Unit)? = null,
    style: PulseButtonStyle = PulseButtonStyle.Standard,
    trailingIcon: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues =
        if (style == PulseButtonStyle.Text) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ContentPadding,
    textAlign: TextAlign? = null,
    icon: (@Composable () -> Unit)? = null,
) {
    val colors = PulseTheme.colors
    val loadingDescription = stringResource(R.string.pulse_ui_loading)
    val actionModifier =
        modifier.semantics {
            if (loading) stateDescription = loadingDescription
        }
    if (style == PulseButtonStyle.Icon) {
        requireNotNull(icon) { "Icon buttons require an icon" }
        IconButton(
            onClick,
            actionModifier.sizeIn(minWidth = PulseTokens.target, minHeight = PulseTokens.target).semantics {
                contentDescription = text
            },
            enabled = enabled && !loading,
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = colors.textPrimary,
                    disabledContentColor = colors.textSecondary,
                ),
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(PulseTokens.medium), color = LocalContentColor.current)
            } else {
                icon()
            }
        }
        return
    }
    val content: @Composable RowScope.() -> Unit = {
        if (loading) {
            CircularProgressIndicator(Modifier.size(PulseTokens.medium), color = LocalContentColor.current)
            Spacer(Modifier.width(PulseTokens.small))
        } else if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(PulseTokens.small))
        }
        PulseText(
            text,
            modifier = if (trailingIcon != null) Modifier.weight(1f, fill = false) else Modifier,
            style = PulseTextStyle.Label,
            textAlign = textAlign,
        )
        if (trailingIcon != null && !loading) {
            Spacer(Modifier.width(PulseTokens.small))
            trailingIcon()
        }
    }
    if (style == PulseButtonStyle.Text) {
        TextButton(
            onClick,
            actionModifier.defaultMinSize(minHeight = PulseTokens.target),
            enabled = enabled && !loading,
            shape = PulseTheme.shapes.pill,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = if (variant == PulseButtonVariant.Destructive) colors.error else colors.action,
                    disabledContentColor = colors.textSecondary,
                ),
            contentPadding = contentPadding,
        ) { content() }
        return
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = PulseTheme.shapes.pill
    val gradient =
        if (variant == PulseButtonVariant.Primary && !PulseTheme.isDark) {
            Brush.horizontalGradient(listOf(colors.gradientStart, colors.gradientEnd))
        } else {
            null
        }
    val container =
        when (variant) {
            PulseButtonVariant.Primary ->
                if (gradient !=
                    null
                ) {
                    Color.Transparent
                } else if (pressed) {
                    colors.primaryPressed
                } else {
                    colors.primary
                }
            PulseButtonVariant.Secondary -> if (pressed) colors.surfaceStrong else colors.surfaceMuted
            PulseButtonVariant.Outline -> if (pressed) colors.chip else Color.Transparent
            PulseButtonVariant.Tertiary -> if (pressed) colors.primary.copy(alpha = 0.1f) else Color.Transparent
            PulseButtonVariant.Destructive -> colors.error
            PulseButtonVariant.Soft -> colors.primarySoft
            PulseButtonVariant.DestructiveSoft -> colors.errorSoft
        }
    val foreground =
        when (variant) {
            PulseButtonVariant.Primary -> colors.onPrimary
            PulseButtonVariant.Secondary, PulseButtonVariant.Outline -> colors.textPrimary
            PulseButtonVariant.Destructive -> colors.background
            PulseButtonVariant.Soft -> colors.onPrimarySoft
            PulseButtonVariant.DestructiveSoft -> colors.error
            PulseButtonVariant.Tertiary -> colors.action
        }
    val active = enabled && !loading
    var buttonModifier = actionModifier.defaultMinSize(minHeight = PulseTokens.target)
    // Web: disabled buttons keep their look at 50% opacity.
    if (!active) buttonModifier = buttonModifier.alpha(DISABLED_ALPHA)
    if (gradient != null) {
        buttonModifier =
            buttonModifier.clip(shape).background(gradient).then(
                if (pressed) Modifier.background(Color.Black.copy(alpha = 0.05f)) else Modifier,
            )
    }
    Button(
        onClick,
        buttonModifier,
        enabled = active,
        shape = shape,
        interactionSource = interaction,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = foreground,
                disabledContainerColor = container,
                disabledContentColor = foreground,
            ),
        contentPadding = contentPadding,
        border = if (variant == PulseButtonVariant.Outline) BorderStroke(OUTLINE_WIDTH, colors.divider) else null,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = PulseTheme.elevation.none,
                pressedElevation = PulseTheme.elevation.none,
                focusedElevation = PulseTheme.elevation.none,
                hoveredElevation = PulseTheme.elevation.none,
                disabledElevation = PulseTheme.elevation.none,
            ),
    ) { content() }
}

private const val DISABLED_ALPHA = 0.5f
private val OUTLINE_WIDTH = 1.5.dp
