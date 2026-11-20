package dev.abhishekan.pulse.feature.garage.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.feature.garage.R

@Composable
internal fun GarageActionPair(
    first: String,
    onFirst: () -> Unit,
    second: String,
    onSecond: () -> Unit,
    enabled: Boolean,
    primaryFirst: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val firstVariant = if (primaryFirst) PulseButtonVariant.Primary else PulseButtonVariant.Secondary
        val secondVariant = if (primaryFirst) PulseButtonVariant.Secondary else PulseButtonVariant.Primary
        if (maxWidth >= 300.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.compact)) {
                PulseButton(
                    first,
                    onFirst,
                    Modifier.weight(1f).heightIn(min = 52.dp),
                    enabled,
                    variant = firstVariant,
                    leadingIcon =
                        if (primaryFirst) {
                            { Icon(painterResource(R.drawable.garage_add), null, Modifier.size(PulseTokens.medium)) }
                        } else {
                            null
                        },
                )
                PulseButton(
                    second,
                    onSecond,
                    Modifier.weight(1f).heightIn(min = 52.dp),
                    enabled,
                    variant = secondVariant,
                    leadingIcon =
                        if (primaryFirst) {
                            { Icon(painterResource(R.drawable.garage_add), null, Modifier.size(PulseTokens.medium)) }
                        } else {
                            null
                        },
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseButton(first, onFirst, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled, variant = firstVariant)
                PulseButton(second, onSecond, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled, variant = secondVariant)
            }
        }
    }
}
