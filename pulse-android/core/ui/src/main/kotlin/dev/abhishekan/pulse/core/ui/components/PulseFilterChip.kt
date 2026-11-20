package dev.abhishekan.pulse.core.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.R

@Composable
fun PulseFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showSelectedIcon: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    FilterChip(
        selected,
        onClick,
        {
            PulseText(label)
        },
        modifier.heightIn(min = PulseTokens.target),
        enabled = enabled,
        shape = PulseTheme.shapes.pill,
        leadingIcon =
            leadingIcon ?: if (selected && showSelectedIcon) {
                {
                    Icon(
                        painterResource(R.drawable.pulse_check),
                        contentDescription = null,
                        modifier = Modifier.size(PulseTokens.medium),
                        tint = PulseTheme.colors.onPrimary,
                    )
                }
            } else {
                null
            },
        trailingIcon =
            trailingIcon ?: if (leadingIcon != null && selected && showSelectedIcon) {
                {
                    Icon(
                        painterResource(R.drawable.pulse_check),
                        contentDescription = null,
                        modifier = Modifier.size(PulseTokens.medium),
                        tint = PulseTheme.colors.onPrimary,
                    )
                }
            } else {
                null
            },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = PulseTheme.colors.surface,
                labelColor = PulseTheme.colors.textPrimary,
                selectedContainerColor = PulseTheme.colors.primary,
                selectedLabelColor = PulseTheme.colors.onPrimary,
                disabledContainerColor = PulseTheme.colors.surfaceMuted,
                disabledLabelColor = PulseTheme.colors.textSecondary,
            ),
    )
}
