package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme

/** A single choice in a selectable group, with a decorative icon above its label. */
@Composable
fun PulseChoiceCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    PulseCard(
        modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        highlighted = selected,
        border = BorderStroke(1.dp, if (selected) PulseTheme.colors.primary else PulseTheme.colors.border),
        contentPadding = PulseTheme.spacing.small,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PulseTheme.spacing.small, Alignment.CenterVertically),
        ) {
            icon()
            PulseText(
                label,
                style = PulseTextStyle.Label,
                textAlign = TextAlign.Center,
                color = if (enabled) PulseTheme.colors.textPrimary else PulseTheme.colors.textSecondary,
            )
        }
    }
}
