package dev.abhishekan.pulse.feature.horizon.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize

internal val PulseWindowSize.horizonGutter
    get() =
        when (this) {
            PulseWindowSize.Compact -> 20.dp
            PulseWindowSize.Medium -> 32.dp
            PulseWindowSize.Expanded -> 48.dp
        }

@Composable
internal fun HorizonTabScroll(
    padding: Dp,
    bottomSpacing: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalPulseContentPadding.current.calculateBottomPadding() + bottomSpacing)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
    ) {
        content()
    }
}
