package dev.abhishekan.pulse.core.ui.components
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme

data class PulseNavigationItem(
    val label: String,
    val icon: @Composable () -> Unit,
)

@Composable
fun PulseBottomNavigationBar(
    items: List<PulseNavigationItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier,
        containerColor = PulseTheme.colors.surface,
        contentColor = PulseTheme.colors.textPrimary,
        tonalElevation = PulseTheme.elevation.none,
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selectedIndex == index,
                {
                    onSelected(index)
                },
                icon = item.icon,
                label = {
                    PulseText(item.label, style = PulseTextStyle.Label)
                },
                colors =
                    NavigationBarItemDefaults.colors(
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
