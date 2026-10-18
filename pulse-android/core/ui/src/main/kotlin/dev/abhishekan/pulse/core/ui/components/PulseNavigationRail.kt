package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme

@Composable
fun PulseNavigationRail(
    items: List<PulseNavigationItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
        containerColor = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
    ) {
        items.forEachIndexed { index, item ->
            NavigationRailItem(
                selected = selectedIndex == index,
                onClick = { onSelected(index) },
                icon = item.icon,
                label = { PulseText(item.label, style = PulseTextStyle.Label) },
                colors =
                    NavigationRailItemDefaults.colors(
                        selectedIconColor = PulseTheme.colors.onPrimary,
                        selectedTextColor = PulseTheme.colors.textPrimary,
                        indicatorColor = PulseTheme.colors.primary,
                        unselectedIconColor = PulseTheme.colors.textSecondary,
                        unselectedTextColor = PulseTheme.colors.textSecondary,
                    ),
            )
        }
    }
}
