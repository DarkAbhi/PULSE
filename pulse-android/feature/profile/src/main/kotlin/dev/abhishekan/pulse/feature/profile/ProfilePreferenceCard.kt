package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.UserCurrency
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
import dev.abhishekan.pulse.core.ui.patterns.PulseFormAction
import dev.abhishekan.pulse.core.ui.patterns.PulseFormActions
import dev.abhishekan.pulse.core.ui.R as CoreUiR

internal enum class ProfilePreference(
    val title: Int,
    val selectLabel: Int,
    val searchLabel: Int,
    val emptyLabel: Int,
) {
    Currency(
        R.string.pulse_profile_currency,
        R.string.pulse_profile_currency_select,
        R.string.pulse_profile_currency_search,
        R.string.pulse_profile_currency_empty,
    ),
    TimeZone(
        R.string.pulse_profile_timezone,
        R.string.pulse_profile_timezone_select,
        R.string.pulse_profile_timezone_search,
        R.string.pulse_profile_timezone_empty,
    ),
    ;

    val options: List<String>
        get() =
            when (this) {
                Currency -> UserCurrency.available
                TimeZone -> UserTimeZone.available
            }

    fun value(state: ProfileUiState): String =
        when (this) {
            Currency -> state.profile?.currency ?: UserCurrency.DEFAULT
            TimeZone -> state.profile?.timezone ?: "UTC"
        }

    fun label(value: String): String =
        when (this) {
            Currency -> UserCurrency.label(value)
            TimeZone -> value.replace('_', ' ')
        }
}

@Composable
internal fun ProfilePreferenceCard(
    preference: ProfilePreference,
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    PulseCard(modifier.fillMaxWidth()) {
        PulseText(stringResource(preference.title), style = PulseTextStyle.Title)
        PulseText(preference.label(preference.value(state)))
        if (preference == ProfilePreference.Currency) {
            PulseText(stringResource(R.string.pulse_profile_currency_explanation))
        }
        PulseButton(
            stringResource(preference.selectLabel),
            onSelect,
            enabled = state.canEditPreferences,
            style = PulseButtonStyle.Text,
        )
        val error =
            when (preference) {
                ProfilePreference.Currency -> state.currencyError
                ProfilePreference.TimeZone -> state.timeZoneError
            }
        error?.let { PulseText(it, color = PulseTheme.colors.error) }
    }
}

@Composable
internal fun ProfilePreferenceDialog(
    preference: ProfilePreference,
    current: String,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(current) }
    PulseDialog(
        onDismissRequest = onDismiss,
        title = { PulseText(stringResource(preference.title), style = PulseTextStyle.Title) },
        text = {
            Column {
                PulseTextField(search, { search = it }, stringResource(preference.searchLabel), Modifier.fillMaxWidth())
                val matches =
                    remember(preference, search) {
                        val query = search.trim().replace('_', ' ')
                        preference.options.filter { preference.label(it).contains(query, ignoreCase = true) }
                    }
                if (matches.isEmpty()) PulseText(stringResource(preference.emptyLabel))
                LazyColumn(Modifier.heightIn(max = 360.dp).selectableGroup()) {
                    items(matches, key = { it }) { option ->
                        PulseRadioButton(selected == option, { selected = option }, preference.label(option), Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            PulseFormActions(
                primary =
                    PulseFormAction(
                        stringResource(CoreUiR.string.pulse_ui_save),
                        { onSave(selected) },
                        enabled = canSave && selected != current,
                    ),
                secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), onDismiss),
            )
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreferenceCardPreview() {
    PulseTheme {
        ProfilePreferenceCard(
            ProfilePreference.Currency,
            ProfileUiState(profileLoad = ProfileLoad.Loaded(Profile(name = "Asha"))),
            onSelect = {},
        )
    }
}
