package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme

@Composable
fun PulseTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = PulseTheme.colors.background,
        edgePadding = 0.dp,
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
