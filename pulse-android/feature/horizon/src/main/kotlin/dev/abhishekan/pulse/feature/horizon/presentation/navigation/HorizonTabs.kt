package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.components.PulseFilterChip
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonGutter
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab

@Composable
internal fun HorizonTabs(
    current: HorizonTab,
    enabled: Boolean,
    width: PulseWindowSize,
    onSelect: (HorizonTab) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = width.horizonGutter)
            .padding(top = if (width == PulseWindowSize.Compact) 16.dp else 32.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizonTab.entries.forEach { tab ->
            PulseFilterChip(
                label = stringResource(tab.label),
                selected = tab == current,
                onClick = { onSelect(tab) },
                enabled = enabled,
                showSelectedIcon = false,
            )
        }
    }
}
