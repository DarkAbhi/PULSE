package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = PulseTokens.controlShape,
        modifier =
            modifier
                .defaultMinSize(
                    minWidth = PulseTokens.target,
                    minHeight = PulseTokens.target,
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
        color = if (selected) colors.primary else colors.surface,
        contentColor =
            if (selected) {
                colors.onPrimary
            } else if (inMonth) {
                colors.onSurface
            } else {
                colors.onSurfaceVariant
            },
        border = if (today) BorderStroke(PulseTokens.focusWidth, colors.primary) else null,
    ) {
        Column(
            Modifier.padding(PulseTokens.tiny),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PulseText(label, style = PulseTextStyle.Label)
            Surface(
                Modifier.size(PulseTokens.tiny),
                shape = CircleShape,
                color =
                    if (!hasActivity) {
                        androidx.compose.ui.graphics.Color.Transparent
                    } else if (selected) {
                        colors.onPrimary
                    } else {
                        colors.tertiary
                    },
            ) { }
        }
    }
}
