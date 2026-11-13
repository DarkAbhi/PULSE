package dev.abhishekan.pulse.core.ui.components

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    backLabel: String? = null,
    backEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    BoxWithConstraints(modifier.fillMaxSize()) {
        val showBar = showTopBar && maxHeight >= PulseTokens.minimumNavigationHeight
        Column(Modifier.fillMaxSize()) {
            if (showBar) {
                PulseTopAppBar(
                    title,
                    actions = actions,
                    navigationIcon = {
                        if (backLabel != null) {
                            PulseButton(
                                backLabel,
                                { if (onBack != null) onBack() else backDispatcher?.onBackPressed() },
                                enabled = backEnabled,
                                style = PulseButtonStyle.Icon,
                            ) { Icon(painterResource(R.drawable.pulse_back), null) }
                        }
                    },
                )
            }
            Box(Modifier.weight(1f)) { content() }
        }
    }
}
