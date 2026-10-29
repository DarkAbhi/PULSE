package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun CurrencyCard(
    state: ProfileUiState,
    onSave: (String) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(UserCurrency.DEFAULT) }
    val current = state.profile?.currency ?: UserCurrency.DEFAULT
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_profile_currency), style = PulseTextStyle.Title)
        PulseText(UserCurrency.label(current))
        PulseText(stringResource(R.string.pulse_profile_currency_explanation))
        PulseButton(stringResource(R.string.pulse_profile_currency_select), {
            selected = current
            search = ""
            open = true
        }, enabled = state.profile != null && !state.loading && !state.busy && !state.uncertain, style = PulseButtonStyle.Text)
        state.error?.let { PulseText(it, color = PulseTheme.colors.error) }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { PulseText(stringResource(R.string.pulse_profile_currency), style = PulseTextStyle.Title) },
            text = {
                Column {
                    PulseTextField(search, { search = it }, stringResource(R.string.pulse_profile_currency_search), Modifier.fillMaxWidth())
                    val matches =
                        remember(
                            search,
                        ) {
                            UserCurrency.available.filter {
                                UserCurrency.label(it).contains(search.trim().replace('_', ' '), ignoreCase = true)
                            }
                        }
                    if (matches.isEmpty()) PulseText(stringResource(R.string.pulse_profile_currency_empty))
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(matches, key = { it }) { zone ->
                            PulseRadioButton(selected == zone, { selected = zone }, UserCurrency.label(zone), Modifier.fillMaxWidth())
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
