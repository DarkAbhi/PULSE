package dev.abhishekan.pulse.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.ProfileRepository
import dev.abhishekan.pulse.core.domain.usecase.AccountValidation
import dev.abhishekan.pulse.core.domain.usecase.ChangePassword
import dev.abhishekan.pulse.core.domain.usecase.CreateAPIKey
import dev.abhishekan.pulse.core.domain.usecase.RevokeAPIKey
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileCurrency
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileName
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileTimeZone
import dev.abhishekan.pulse.core.ui.DraftSource
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.UnsavedChanges
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profile: ProfileRepository,
    private val saveName: SaveProfileName,
    private val password: ChangePassword,
    private val createKey: CreateAPIKey,
    private val revokeKey: RevokeAPIKey,
    private val saved: SavedStateHandle,
    private val saveCurrency: SaveProfileCurrency,
    private val saveTimeZone: SaveProfileTimeZone,
    unsavedChanges: UnsavedChanges = UnsavedChanges(),
) : ViewModel() {
    private val state = MutableStateFlow(restoreState())
    internal val uiState = state.asStateFlow()

    private val eventChannel = Channel<ProfileEvent>(Channel.BUFFERED)
    internal val events = eventChannel.receiveAsFlow()

    private var secretGeneration = 0L
    private var profileJob: Job? = null
    private var keysJob: Job? = null

    init {
        unsavedChanges.track(
            DraftSource.Profile,
            state.map { it.editor == AccountEditor.Password && it.passwords.isDirty },
            viewModelScope,
        )
        refreshAll()
    }

    internal fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Refresh -> refreshAll()
            ProfileAction.RefreshProfile -> refreshProfile()
            ProfileAction.RefreshKeys -> refreshKeys()
            ProfileAction.RefreshReview -> refreshReview()
            ProfileAction.OpenInitialNameEditor -> openInitialNameEditor()
            is ProfileAction.Edit -> edit(action.editor)
            is ProfileAction.ChangeName -> changeName(action.value)
            is ProfileAction.ChangeKeyName -> changeKeyName(action.value)
            is ProfileAction.ChangePasswordField -> changePasswordField(action)
            ProfileAction.SaveEditor -> saveEditor()
            ProfileAction.CloseEditor -> closeEditor()
            is ProfileAction.SaveCurrency -> saveCurrencyPreference(action.value)
            is ProfileAction.SaveTimeZone -> saveTimeZonePreference(action.value)
            is ProfileAction.RevokeKey -> revoke(action.id)
            ProfileAction.AcknowledgeReview -> acknowledgeReview()
            ProfileAction.ClearSecrets -> clearSecrets()
            ProfileAction.CloseToken -> closeToken()
            is ProfileAction.TokenCopied -> tokenCopied(action.success)
        }
    }

    private fun restoreState(): ProfileUiState {
        val pending = saved.get<String>("pending")
        val review =
            when {
                pending != null -> ProfileMutation.entries.find { it.savedValue == pending } ?: ProfileMutation.Key
                saved.get<Boolean>("tokenNeedsReview") == true -> ProfileMutation.Key
                else -> null
            }
        return ProfileUiState(
            editor = if (saved.get<Boolean>("editingName") == true) AccountEditor.Name else null,
            name = saved["name"] ?: "",
            keyName = saved["keyName"] ?: "",
            review = review,
        )
    }

    private fun refreshAll() {
        refreshProfile()
        refreshKeys()
    }

    private fun refreshProfile(restart: Boolean = false) {
        if (!restart && profileJob?.isActive == true) return
        profileJob?.cancel()
        state.update { it.copy(profileLoad = ProfileLoad.Loading(it.profile)) }
        profileJob =
            viewModelScope.launch {
                val result = profile.profile()
                ensureActive()
                state.update {
                    when (result) {
                        is Outcome.Success -> it.copy(profileLoad = ProfileLoad.Loaded(result.value))
                        is Outcome.Error -> it.copy(profileLoad = ProfileLoad.Failed(result.failure, it.profile))
                    }
                }
            }
    }

    private fun refreshKeys(restart: Boolean = false) {
        if (!restart && keysJob?.isActive == true) return
        keysJob?.cancel()
        state.update { it.copy(keysLoad = ProfileLoad.Loading(it.keysLoad.data)) }
        keysJob =
            viewModelScope.launch {
                val result = profile.keys()
                ensureActive()
                state.update {
                    when (result) {
                        is Outcome.Success -> it.copy(keysLoad = ProfileLoad.Loaded(result.value))
                        is Outcome.Error -> it.copy(keysLoad = ProfileLoad.Failed(result.failure, it.keysLoad.data))
                    }
                }
            }
    }

    private fun refreshReview(restart: Boolean = false) {
        val review = state.value.review ?: return
        if (review.reviewsProfile) refreshProfile(restart)
        if (review.reviewsKeys) refreshKeys(restart)
    }

    private fun acknowledgeReview() {
        if (!state.value.reviewed || state.value.busy) return
        saved.remove<String>("pending")
        saved.remove<Boolean>("tokenNeedsReview")
        state.update { it.copy(review = null, error = null, currencyError = null, timeZoneError = null) }
    }

    private fun openInitialNameEditor() {
        if (saved.get<Boolean>("initialNameEditorConsumed") == true) return
        saved["initialNameEditorConsumed"] = true
        edit(AccountEditor.Name)
    }

    private fun edit(editor: AccountEditor) {
        val current = state.value
        if (!current.canMutate || current.editor != null) return
        val name = if (editor == AccountEditor.Name) current.profile?.name.orEmpty() else current.name
        if (editor == AccountEditor.Name) {
            saved["editingName"] = true
            saved["name"] = name
        }
        state.update { it.copy(editor = editor, name = name, error = null) }
    }

    private fun changeName(value: String) {
        if (state.value.editor != AccountEditor.Name || !state.value.canMutate) return
        saved["name"] = value
        state.update { it.copy(name = value, error = null) }
    }

    private fun changeKeyName(value: String) {
        if (state.value.editor != AccountEditor.Key || !state.value.canMutate) return
        saved["keyName"] = value
        state.update { it.copy(keyName = value, error = null) }
    }

    private fun changePasswordField(action: ProfileAction.ChangePasswordField) {
        if (state.value.editor != AccountEditor.Password || !state.value.canMutate) return
        state.update { it.copy(passwords = it.passwords.change(action.field, action.value), error = null) }
    }

    private fun clearSavedDraft() {
        saved.remove<Boolean>("editingName")
        saved.remove<String>("name")
        saved.remove<String>("keyName")
    }

    private fun closeEditor() {
        if (state.value.busy) return
        clearSavedDraft()
        state.update { it.copy(editor = null, name = "", keyName = "", passwords = PasswordFields(), error = null) }
    }

    private fun clearSecrets() {
        secretGeneration++
        val hadToken = state.value.token != null
        state.update {
            it.copy(
                token = null,
                tokenCopyMessage = null,
                passwords = PasswordFields(),
                editor = it.editor.takeIf { editor -> editor == AccountEditor.Name },
                review = if (hadToken) ProfileMutation.Key else it.review,
            )
        }
    }

    private fun closeToken() {
        saved.remove<Boolean>("tokenNeedsReview")
        state.update { it.copy(token = null, tokenCopyMessage = null) }
    }

    private fun tokenCopied(success: Boolean) {
        if (state.value.token == null) return
        state.update {
            it.copy(tokenCopyMessage = uiText(if (success) R.string.pulse_profile_copied else R.string.pulse_profile_copy_failed))
        }
    }

    private fun saveEditor() {
        val current = state.value
        if (!current.canMutate) return
        when (current.editor) {
            AccountEditor.Name -> {
                if (!AccountValidation.validName(current.name)) {
                    showEditorError(R.string.pulse_profile_name_validation)
                    return
                }
                mutate(ProfileMutation.Name, R.string.pulse_profile_saved, { saveName(current.name) }) { updated ->
                    copy(profileLoad = ProfileLoad.Loaded(updated))
                }
            }
            AccountEditor.Password -> {
                val draft = current.passwords
                if (!AccountValidation.validPassword(draft.current, draft.next, draft.confirmation)) {
                    showEditorError(R.string.pulse_profile_password_validation)
                    return
                }
                mutate(ProfileMutation.Password, R.string.pulse_profile_password_changed, {
                    password(draft.current, draft.next, draft.confirmation)
                }) { this }
            }
            AccountEditor.Key -> {
                if (!AccountValidation.validName(current.keyName)) {
                    showEditorError(R.string.pulse_profile_name_validation)
                    return
                }
                mutate(ProfileMutation.Key, request = { createKey(current.keyName) }) { created ->
                    copy(token = created.takeIf { review == null })
                }
            }
            null -> Unit
        }
    }

    private fun showEditorError(message: Int) {
        state.update { it.copy(error = uiText(message)) }
    }

    private fun saveTimeZonePreference(value: String) {
        if (!state.value.canEditPreferences) return
        mutate(ProfileMutation.TimeZone, R.string.pulse_profile_saved, { saveTimeZone(value) }) { updated ->
            copy(profileLoad = ProfileLoad.Loaded(checkNotNull(profile).copy(timezone = updated)))
        }
    }

    private fun saveCurrencyPreference(value: String) {
        if (!state.value.canEditPreferences) return
        mutate(ProfileMutation.Currency, R.string.pulse_profile_saved, { saveCurrency(value) }) { updated ->
            copy(profileLoad = ProfileLoad.Loaded(checkNotNull(profile).copy(currency = updated)))
        }
    }

    private fun revoke(id: Long) {
        if (!state.value.canMutate) return
        mutate(ProfileMutation.Revoke, R.string.pulse_profile_revoked, { revokeKey(id) }) {
            val remaining = keys.filterNot { key -> key.id == id }
            copy(keysLoad = ProfileLoad.Loaded(remaining))
        }
    }

    private fun <T> mutate(
        mutation: ProfileMutation,
        successMessage: Int? = null,
        request: suspend () -> Outcome<T>,
        applySuccess: ProfileUiState.(T) -> ProfileUiState,
    ) {
        saved["pending"] = mutation.savedValue
        state.update { it.withError(mutation, null).copy(busy = true) }
        val generation = secretGeneration
        viewModelScope.launch {
            val result = request()
            ensureActive()
            when (result) {
                is Outcome.Success -> {
                    val lostToken = mutation == ProfileMutation.Key && generation != secretGeneration
                    if (mutation == ProfileMutation.Key) saved["tokenNeedsReview"] = true
                    saved.remove<String>("pending")
                    if (mutation.editor != null) clearSavedDraft()
                    state.update {
                        val next = it.copy(busy = false, review = mutation.takeIf { lostToken })
                        val updated = next.applySuccess(result.value)
                        if (mutation.editor == null) {
                            updated
                        } else {
                            updated.copy(editor = null, name = "", keyName = "", passwords = PasswordFields())
                        }
                    }
                    refreshProfile(restart = true)
                    refreshKeys(restart = true)
                    successMessage?.let { eventChannel.send(ProfileEvent(uiText(it))) }
                }
                is Outcome.Error -> handleMutationError(mutation, result.failure)
            }
        }
    }

    private suspend fun handleMutationError(
        mutation: ProfileMutation,
        failure: Failure,
    ) {
        val needsReview =
            when (failure) {
                Failure.Connection, Failure.Timeout, Failure.Server, Failure.Malformed -> true
                else -> false
            }
        if (!needsReview) saved.remove<String>("pending")
        val error =
            if (mutation == ProfileMutation.Password && failure == Failure.InvalidCredentials) {
                uiText(R.string.pulse_profile_wrong_password)
            } else {
                failure.message
            }
        state.update {
            val next = it.withError(mutation, error).copy(busy = false, review = mutation.takeIf { needsReview })
            if (needsReview && mutation == ProfileMutation.Password) {
                next.copy(editor = null, passwords = PasswordFields())
            } else {
                next
            }
        }
        if (needsReview) refreshReview(restart = true)
        if (!needsReview && mutation == ProfileMutation.Revoke) eventChannel.send(ProfileEvent(error))
    }

    private fun ProfileUiState.withError(
        mutation: ProfileMutation,
        error: UiText?,
    ): ProfileUiState =
        when (mutation) {
            ProfileMutation.Currency -> copy(currencyError = error)
            ProfileMutation.TimeZone -> copy(timeZoneError = error)
            ProfileMutation.Revoke -> this
            else -> copy(error = error)
        }

    override fun onCleared() {
        clearSecrets()
    }
}
