package dev.abhishekan.pulse.core.ui.components


import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseTopAppBar(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}, navigationIcon: @Composable () -> Unit = {}) {
    TopAppBar(title = { PulseText(title, style = PulseTextStyle.Title) }, modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background), actions = actions, navigationIcon = navigationIcon)
}

@Composable
fun PulseTopAppBar(title: UiText, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}, navigationIcon: @Composable () -> Unit = {}) = PulseTopAppBar(title.asString(), modifier, actions, navigationIcon)
