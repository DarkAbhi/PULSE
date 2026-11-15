package dev.abhishekan.pulse.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun LoginIntroduction(
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        PulseText(
            stringResource(R.string.pulse_auth_pulse),
            style = PulseTextStyle.Label,
            color = PulseTheme.colors.action,
        )
        PulseText(
            stringResource(
                if (expanded) {
                    R.string.pulse_auth_your_everyday_in_one_place
                } else {
                    R.string.pulse_auth_your_everyday_considered
                },
            ),
            Modifier.semantics { heading() },
            PulseTextStyle.Display,
        )
        PulseText(
            stringResource(R.string.pulse_auth_a_private_space_for_your_fitness_finances_meals_and),
            color = PulseTheme.colors.textSecondary,
        )
    }
}
