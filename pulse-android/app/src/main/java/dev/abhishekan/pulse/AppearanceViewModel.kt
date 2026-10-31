package dev.abhishekan.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.repository.PreferencesRepository
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(
    val theme: ThemePreference = ThemePreference.System,
    val error: UiText? = null,
)

class AppearanceViewModel(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val error = MutableStateFlow<UiText?>(null)
    val uiState =
        combine(preferences.theme, error) { theme, message ->
            AppearanceUiState(theme, message)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch {
            error.value =
                when (preferences.setTheme(theme)) {
                    is Outcome.Success -> null
                    is Outcome.Error ->
                        uiText(
                            R.string.pulse_app_couldn_t_save_appearance_please_try_again,
                        )
                }
        }
    }
}
