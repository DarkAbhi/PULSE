package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens

@Composable
fun PulseTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pills: Boolean = false,
    edgePadding: Dp = 0.dp,
) {
    if (pills) {
        PulsePillTabs(labels, selectedIndex, onSelect, modifier, enabled, edgePadding)
        return
    }
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = PulseTheme.colors.background,
        edgePadding = edgePadding,
        divider = { PulseHorizontalDivider() },
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = false),
                color = PulseTheme.colors.primary,
            )
        },
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Tab(
                selected = selected,
                onClick = { onSelect(index) },
                enabled = enabled,
                text = {
                    PulseText(
                        label,
                        style = PulseTextStyle.Label,
                        color = if (selected) PulseTheme.colors.primary else PulseTheme.colors.textSecondary,
                    )
                },
            )
        }
    }
}

@Composable
private fun PulsePillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    edgePadding: Dp,
) {
    val colors = PulseTheme.colors
    Row(
        modifier.horizontalScroll(rememberScrollState()).padding(horizontal = edgePadding).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Surface(
                selected = selected,
                onClick = { onSelect(index) },
                modifier = Modifier.semantics { role = Role.Tab },
                enabled = enabled,
                shape = PulseTheme.shapes.pill,
                color = if (selected) colors.primary else colors.surface,
                contentColor = if (selected) colors.onPrimary else colors.textPrimary,
                border = if (selected) null else BorderStroke(1.5.dp, colors.divider),
            ) {
                Box(
                    Modifier.heightIn(min = PulseTokens.target).padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PulseText(
                        label,
                        style = PulseTheme.typography.title3.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    )
                }
            }
        }
    }
}
