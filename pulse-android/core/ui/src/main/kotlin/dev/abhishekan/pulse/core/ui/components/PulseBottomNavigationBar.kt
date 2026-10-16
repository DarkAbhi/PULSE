package dev.abhishekan.pulse.core.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class PulseNavigationItem(val label: String, val icon: @Composable () -> Unit)

@Composable
fun PulseBottomNavigationBar(items: List<PulseNavigationItem>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier, containerColor = MaterialTheme.colorScheme.surface) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(selectedIndex == index, { onSelected(index) }, icon = item.icon, label = { PulseText(item.label, style = PulseTextStyle.Label) })
        }
    }
}
