package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseCalendarDay(
    label: String,
    description: String,
    selected: Boolean,
    today: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inMonth: Boolean = true,
) {
    val colors = PulseTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = 44.dp,
                    minHeight = 56.dp,
                ).clearAndSetSemantics {
                    contentDescription = description
                    this.selected = selected
                    role = Role.Button
                    if (enabled) {
                        onClick {
                            onClick()
                            true
                        }
                    } else {
                        disabled()
                    }
                },
        color = Color.Transparent,
    ) {
        // The column owns the tap target; the marker stays circular at every width.
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (today) {
                    Box(Modifier.size(44.dp).border(PulseTokens.focusWidth, colors.action, CircleShape))
                }
                Surface(
                    Modifier.size(40.dp),
                    shape = CircleShape,
                    color =
                        when {
                            selected -> colors.primary
                            hasActivity -> colors.successSoft
                            else -> Color.Transparent
                        },
                    contentColor =
                        when {
                            selected -> colors.onPrimary
                            hasActivity -> colors.success
                            inMonth -> colors.textPrimary
                            else -> colors.textSecondary
                        },
                    border = if (hasActivity && !selected) BorderStroke(PulseTokens.focusWidth, colors.success) else null,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        PulseText(label, style = PulseTextStyle.Label)
                    }
                }
                // Overlay the dot so activity never shifts the number off center.
                if (hasActivity) {
                    Surface(
                        Modifier.align(Alignment.BottomCenter).offset(y = 2.dp).size(8.dp),
                        shape = CircleShape,
                        color = colors.success,
                        border = BorderStroke(2.dp, colors.surface),
                    ) {}
                }
            }
        }
    }
}
