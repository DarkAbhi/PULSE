package dev.abhishekan.pulse.feature.profile
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.ui.UiText

enum class AccountEditor { Name, Password, Key }

// Sensitive fields never participate in generated toString, saved state, or persistence.
class PasswordFields(
    val current: String = "",
    val next: String = "",
    val confirmation: String = "",
)

data class ProfileUiState(
    val profile: Profile? = null,
    val loading: Boolean = true,
    val profileError: Failure? = null,
    val keys: List<APIKey> = emptyList(),
    val keysLoading: Boolean = false,
    val keysLoaded: Boolean = false,
    val keysError: Failure? = null,
    val editor: AccountEditor? = null,
    val name: String = "",
    val keyName: String = "",
    val passwords: PasswordFields = PasswordFields(),
    val busy: Boolean = false,
    val error: UiText? = null,
    val message: UiText? = null,
    val token: CreatedAPIKey? = null,
    val uncertain: Boolean = false,
    val passwordUncertain: Boolean = false,
    val reviewed: Boolean = false,
)
