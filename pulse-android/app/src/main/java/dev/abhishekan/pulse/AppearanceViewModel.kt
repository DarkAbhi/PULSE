package dev.abhishekan.pulse

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.usecase.ObserveTheme
import dev.abhishekan.pulse.core.domain.usecase.SetTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(val theme: ThemePreference = ThemePreference.System, val error: UiText? = null)

class AppearanceViewModel(observeTheme: ObserveTheme, private val setTheme: SetTheme) : ViewModel() {
    private val error = MutableStateFlow<UiText?>(null)
    val uiState = combine(observeTheme(), error) { theme, message -> AppearanceUiState(theme, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch {
            error.value = when (setTheme(theme)) {
                is Outcome.Success -> null
                is Outcome.Error -> uiText(R.string.pulse_app_couldn_t_save_appearance_please_try_again)
            }
        }
    }
}
