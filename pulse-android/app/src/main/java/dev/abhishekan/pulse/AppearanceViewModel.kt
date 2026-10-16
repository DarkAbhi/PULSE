package dev.abhishekan.pulse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.ObserveTheme
import dev.abhishekan.pulse.core.domain.SetTheme
import dev.abhishekan.pulse.core.domain.ThemePreference
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppearanceUiState(val theme: ThemePreference = ThemePreference.System, val error: String? = null)

class AppearanceViewModel(observeTheme: ObserveTheme, private val setTheme: SetTheme) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    val uiState = combine(observeTheme(), error) { theme, message -> AppearanceUiState(theme, message) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun selectTheme(theme: ThemePreference) {
        viewModelScope.launch {
            try {
                setTheme(theme)
                error.value = null
            } catch (_: IOException) {
                error.value = "Couldn’t save appearance. Please try again."
            }
        }
    }
}
