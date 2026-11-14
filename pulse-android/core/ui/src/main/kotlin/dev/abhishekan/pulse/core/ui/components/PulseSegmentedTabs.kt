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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
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
    Layout(
        modifier =
            modifier
                .clip(PulseTheme.shapes.pill)
                .background(colors.surface)
                .border(BorderStroke(PulseTokens.focusWidth / 2, colors.divider), PulseTheme.shapes.pill)
                .horizontalScroll(rememberScrollState())
                .padding(PulseTokens.tiny)
                .selectableGroup(),
        content = {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                Row(
                    Modifier
                        .heightIn(min = PulseTokens.target)
                        .clip(PulseTheme.shapes.pill)
                        .background(if (selected) colors.primary else Color.Transparent)
                        .selectable(selected = selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(index) })
                        .padding(
                            horizontal = if (compact) PulseTheme.spacing.compact else PulseTokens.cardPadding,
                            vertical = PulseTheme.spacing.compact,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
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
        },
    ) { measurables, constraints ->
        val spacing = PulseTokens.tiny.roundToPx()
        val gaps = spacing * (measurables.size - 1).coerceAtLeast(0)
        val tabWidth =
            if (measurables.isEmpty()) {
                0
            } else {
                maxOf(
                    measurables.maxOf { it.maxIntrinsicWidth(Constraints.Infinity) },
                    (constraints.minWidth - gaps).coerceAtLeast(0) / measurables.size,
                )
            }
        val remainder = (constraints.minWidth - gaps - tabWidth * measurables.size).coerceAtLeast(0)
        val tabs =
            measurables.mapIndexed { index, measurable ->
                measurable.measure(Constraints.fixedWidth(tabWidth + if (index < remainder) 1 else 0))
            }
        val width = (tabs.sumOf { it.width } + gaps).coerceAtLeast(constraints.minWidth)
        val height = (tabs.maxOfOrNull { it.height } ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(width, height) {
            var x = 0
            tabs.forEach { tab ->
                tab.placeRelative(x, (height - tab.height) / 2)
                x += tab.width + spacing
            }
        }
    }
}
