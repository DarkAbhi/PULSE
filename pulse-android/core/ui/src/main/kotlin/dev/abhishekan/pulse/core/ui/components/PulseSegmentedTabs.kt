package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

/** A single selection tab group that can scroll on narrow screens or with large text. */
@Composable
fun PulseSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val colors = PulseTheme.colors
    Row(
        modifier
            .clip(PulseTheme.shapes.pill)
            .background(colors.surface)
            .border(BorderStroke(PulseTokens.focusWidth / 2, colors.divider), PulseTheme.shapes.pill)
            .horizontalScroll(rememberScrollState())
            .padding(PulseTokens.tiny)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Row(
                Modifier
                    .heightIn(min = PulseTokens.target)
                    .clip(PulseTheme.shapes.pill)
                    .background(if (selected) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(
                        horizontal = if (compact) PulseTheme.spacing.compact else PulseTokens.cardPadding,
                        vertical = PulseTheme.spacing.compact,
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                PulseText(
                    label,
                    style = PulseTextStyle.Label,
                    color =
                        when {
                            !enabled -> colors.textSecondary
                            selected -> colors.onPrimary
                            else -> colors.textPrimary
                        },
                )
            }
        }
    }
}
