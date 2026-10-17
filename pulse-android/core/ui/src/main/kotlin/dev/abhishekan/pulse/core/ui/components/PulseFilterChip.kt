package dev.abhishekan.pulse.core.ui.components


import dev.abhishekan.pulse.core.ui.asString

import dev.abhishekan.pulse.core.ui.UiText
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.abhishekan.pulse.core.ui.PulseTokens
@Composable
fun PulseFilterChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilterChip(selected, onClick, { PulseText(label) }, modifier.heightIn(min = PulseTokens.target), enabled = enabled)
}

@Composable
fun PulseFilterChip(label: UiText, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) = PulseFilterChip(label.asString(), selected, onClick, modifier, enabled)
