package dev.abhishekan.pulse.core.ui.components
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = {
            PulseText(
                title,
                style = PulseTextStyle.Title,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = PulseTheme.colors.background,
                scrolledContainerColor = PulseTheme.colors.background,
                titleContentColor = PulseTheme.colors.textPrimary,
                navigationIconContentColor = PulseTheme.colors.textPrimary,
                actionIconContentColor = PulseTheme.colors.textPrimary,
            ),
        actions = actions,
        navigationIcon = navigationIcon,
    )
}
