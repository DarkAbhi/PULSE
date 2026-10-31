package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun TimeZoneCard(
    state: ProfileUiState,
    onSave: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf("UTC") }
    val current = state.profile?.timezone ?: "UTC"
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_profile_timezone), style = PulseTextStyle.Title)
        PulseText(current.replace('_', ' '))
        PulseButton(stringResource(R.string.pulse_profile_timezone_select), {
            selected = current
            search = ""
            open = true
        }, enabled = state.profile != null && !state.loading && !state.busy && !state.uncertain, style = PulseButtonStyle.Text)
        state.error?.let { PulseText(it, color = PulseTheme.colors.error) }
    }
    if (open) {
        PulseDialog(
            onDismissRequest = { open = false },
            title = { PulseText(stringResource(R.string.pulse_profile_timezone), style = PulseTextStyle.Title) },
            text = {
                Column {
                    PulseTextField(search, { search = it }, stringResource(R.string.pulse_profile_timezone_search), Modifier.fillMaxWidth())
                    val matches =
                        remember(
                            search,
                        ) {
                            UserTimeZone.available.filter {
                                it
                                    .replace(
                                        '_',
                                        ' ',
                                    ).contains(search.trim().replace('_', ' '), ignoreCase = true)
                            }
                        }
                    if (matches.isEmpty()) PulseText(stringResource(R.string.pulse_profile_timezone_empty))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(matches, key = { it }) { zone ->
                            PulseRadioButton(selected == zone, { selected = zone }, zone.replace('_', ' '), Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                PulseButton(stringResource(R.string.pulse_profile_save), {
                    open = false
                    onSave(selected)
                }, enabled = selected != current, style = PulseButtonStyle.Text)
            },
            dismissButton = { PulseButton(stringResource(R.string.pulse_profile_cancel), { open = false }, style = PulseButtonStyle.Text) },
        )
    }
}
