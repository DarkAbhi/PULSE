package dev.abhishekan.pulse.feature.profile

import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

internal enum class AccountEditor { Name, Password, Key }

internal enum class PasswordField { Current, Next, Confirmation }

// Passwords must stay in memory and out of generated toString output.
internal class PasswordFields(
    val current: String = "",
    val next: String = "",
    val confirmation: String = "",
) {
    val isDirty: Boolean
        get() = current.isNotEmpty() || next.isNotEmpty() || confirmation.isNotEmpty()

    fun change(
        field: PasswordField,
        value: String,
    ): PasswordFields =
        when (field) {
            PasswordField.Current -> PasswordFields(value, next, confirmation)
            PasswordField.Next -> PasswordFields(current, value, confirmation)
            PasswordField.Confirmation -> PasswordFields(current, next, value)
        }
}

internal sealed interface ProfileLoad<out T> {
    val data: T?

    data class Loading<T>(
        override val data: T? = null,
    ) : ProfileLoad<T>

    data class Loaded<T>(
        override val data: T,
    ) : ProfileLoad<T>

    data class Failed<T>(
        val failure: Failure,
        override val data: T?,
    ) : ProfileLoad<T>
}

internal enum class ProfileMutation(
    val savedValue: String,
    val editor: AccountEditor? = null,
) {
    Name("Name", AccountEditor.Name),
    Password("Password", AccountEditor.Password),
    Key("Key", AccountEditor.Key),
    Revoke("revoke"),
    Currency("Currency"),
    TimeZone("TimeZone"),
    ;

    val reviewsProfile: Boolean
        get() = this == Name || this == Currency || this == TimeZone

    val reviewsKeys: Boolean
        get() = this == Key || this == Revoke

    val reviewMessage: UiText
        get() =
            uiText(
                when (this) {
                    Name -> R.string.pulse_profile_name_uncertain
                    Password -> R.string.pulse_profile_password_uncertain
                    Currency -> R.string.pulse_profile_currency_uncertain
                    TimeZone -> R.string.pulse_profile_timezone_uncertain
                    Key, Revoke -> R.string.pulse_profile_uncertain
                },
            )
}

internal data class ProfileUiState(
    val profileLoad: ProfileLoad<Profile> = ProfileLoad.Loading(),
    val keysLoad: ProfileLoad<List<APIKey>> = ProfileLoad.Loading(),
    val editor: AccountEditor? = null,
    val name: String = "",
    val keyName: String = "",
    val passwords: PasswordFields = PasswordFields(),
    val busy: Boolean = false,
    val error: UiText? = null,
    val currencyError: UiText? = null,
    val timeZoneError: UiText? = null,
    val review: ProfileMutation? = null,
    val token: CreatedAPIKey? = null,
    val tokenCopyMessage: UiText? = null,
) {
    val profile get() = profileLoad.data
    val loading get() = profileLoad is ProfileLoad.Loading
    val profileError get() = (profileLoad as? ProfileLoad.Failed)?.failure
    val keys get() = keysLoad.data.orEmpty()
    val keysLoading get() = keysLoad is ProfileLoad.Loading
    val keysLoaded get() = keysLoad.data != null
    val keysError get() = (keysLoad as? ProfileLoad.Failed)?.failure
    val uncertain get() = review != null
    val passwordUncertain get() = review == ProfileMutation.Password
    val reviewed: Boolean
        get() =
            when (review) {
                ProfileMutation.Name, ProfileMutation.Currency, ProfileMutation.TimeZone -> profileLoad is ProfileLoad.Loaded
                ProfileMutation.Key, ProfileMutation.Revoke -> keysLoad is ProfileLoad.Loaded
                ProfileMutation.Password -> true
                null -> false
            }

    val canMutate get() = !busy && !uncertain && token == null
    val canEditPreferences get() = canMutate && !loading && profile != null
    val editorDirty: Boolean
        get() =
            when (editor) {
                AccountEditor.Name -> name != profile?.name.orEmpty()
                AccountEditor.Password -> passwords.isDirty
                AccountEditor.Key -> keyName.isNotBlank()
                null -> false
            }
}
