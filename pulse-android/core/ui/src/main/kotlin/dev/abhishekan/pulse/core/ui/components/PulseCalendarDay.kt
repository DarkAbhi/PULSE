package dev.abhishekan.pulse.core.ui.components


import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseCalendarDay(label: String, description: String, selected: Boolean, today: Boolean, hasActivity: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, inMonth: Boolean = true) {
    val colors = MaterialTheme.colorScheme
    Surface(onClick = onClick, enabled = enabled, shape = PulseTokens.controlShape,
        modifier = modifier.defaultMinSize(minWidth = PulseTokens.target, minHeight = PulseTokens.target).clearAndSetSemantics {
            contentDescription = description
            this.selected = selected
            role = Role.Button
            if (enabled) onClick { onClick(); true } else disabled()
        },
        color = if (selected) colors.primary else colors.surface,
        contentColor = if (selected) colors.onPrimary else if (inMonth) colors.onSurface else colors.onSurfaceVariant,
        border = if (today) BorderStroke(PulseTokens.focusWidth, colors.primary) else null) {
        Column(Modifier.padding(PulseTokens.tiny), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            PulseText(label, style = PulseTextStyle.Label)
            Surface(Modifier.size(PulseTokens.tiny), shape = CircleShape,
                color = if (!hasActivity) androidx.compose.ui.graphics.Color.Transparent else if (selected) colors.onPrimary else colors.tertiary) { }
        }
    }
}

@Composable
fun PulseCalendarDay(label: String, description: UiText, selected: Boolean, today: Boolean, hasActivity: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, inMonth: Boolean = true) = PulseCalendarDay(label, description.asString(), selected, today, hasActivity, onClick, modifier, enabled, inMonth)
