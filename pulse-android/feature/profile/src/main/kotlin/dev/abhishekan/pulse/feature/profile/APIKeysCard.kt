package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun APIKeysCard(
    state: ProfileUiState,
    timeZone: ZoneId,
    pullRefreshing: Boolean,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    revoke: (APIKey) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(timeZone, locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale).withZone(timeZone) }
    PulseCard(modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_profile_keys), Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseText(stringResource(R.string.pulse_profile_keys_description))
        if (state.keysLoading && !state.keysLoaded && !pullRefreshing) {
            PulseCenteredLoadingState(stringResource(R.string.pulse_profile_keys), Modifier.fillMaxWidth())
        }
        state.keysError?.let { PulseErrorState(it.message, { onAction(ProfileAction.RefreshKeys) }) }
        if (state.keysLoaded &&
            state.keys.isEmpty()
        ) {
            PulseEmptyState(stringResource(R.string.pulse_profile_no_keys), stringResource(R.string.pulse_profile_no_keys_body))
        }
        state.keys.forEach { key ->
            PulseText(key.name, style = PulseTextStyle.Label)
            PulseText(
                stringResource(
                    R.string.pulse_profile_key_metadata,
                    key.prefix,
                    formatter.format(key.createdAt),
                ),
            )
            PulseButton(
                stringResource(R.string.pulse_profile_revoke, key.name),
                { revoke(key) },
                enabled = state.canMutate,
                style = PulseButtonStyle.Text,
            )
        }
        PulseButton(
            stringResource(R.string.pulse_profile_create_key),
            { onAction(ProfileAction.Edit(AccountEditor.Key)) },
            enabled = state.canMutate,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun APIKeysCardPreview() {
    PulseTheme {
        APIKeysCard(
            state = ProfileUiState(keysLoad = ProfileLoad.Loaded(emptyList())),
            timeZone = ZoneId.of("UTC"),
            pullRefreshing = false,
            onAction = {},
            revoke = {},
        )
    }
}
