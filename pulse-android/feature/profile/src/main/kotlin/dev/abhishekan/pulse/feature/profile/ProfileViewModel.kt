package dev.abhishekan.pulse.feature.profile
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.CreatedAPIKey
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.usecase.AccountValidation
import dev.abhishekan.pulse.core.domain.usecase.ChangePassword
import dev.abhishekan.pulse.core.domain.usecase.CreateAPIKey
import dev.abhishekan.pulse.core.domain.usecase.GetAPIKeys
import dev.abhishekan.pulse.core.domain.usecase.GetAccountProfile
import dev.abhishekan.pulse.core.domain.usecase.RevokeAPIKey
import dev.abhishekan.pulse.core.domain.usecase.SaveProfileName
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getProfile: GetAccountProfile,
    private val saveName: SaveProfileName,
    private val password: ChangePassword,
    private val getKeys: GetAPIKeys,
    private val createKey: CreateAPIKey,
    private val revokeKey: RevokeAPIKey,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val state =
        MutableStateFlow(
            ProfileUiState(
                name = saved["name"] ?: "",
                keyName = saved["keyName"] ?: "",
                editor = if (saved.get<Boolean>("editingName") == true) AccountEditor.Name else null,
                uncertain = saved.get<String>("pending") != null || saved.get<Boolean>("tokenNeedsReview") == true,
                passwordUncertain = saved.get<String>("pending") == "Password",
                message =
                    if (saved.get<String>("pending") !=
                        null ||
                        saved.get<Boolean>("tokenNeedsReview") == true
                    ) {
                        uiText(
                            if (saved.get<String>("pending") ==
                                "Password"
                            ) {
                                R.string.pulse_profile_password_uncertain
                            } else if (saved.get<String>("pending") ==
                                "Name"
                            ) {
                                R.string.pulse_profile_name_uncertain
                            } else {
                                R.string.pulse_profile_uncertain
                            },
                        )
                    } else {
                        null
                    },
            ),
        )
    val uiState = state.asStateFlow()

    fun consumeMessage() {
        state.update { it.copy(message = null) }
    }

    private var secretGeneration = 0L
    private var profileJob: Job? = null
    private var keysJob: Job? = null

    init {
        refreshProfile()
        refreshKeys()
    }

    fun refreshProfile() {
        if (profileJob?.isActive == true) return
        state.update { it.copy(loading = true, profileError = null) }
        profileJob =
            viewModelScope.launch {
                when (val result = getProfile()) {
                    is Outcome.Success -> state.update { it.copy(profile = result.value, loading = false) }
                    is Outcome.Error -> state.update { it.copy(profileError = result.failure, loading = false) }
                }
                reviewReady()
            }
    }

    fun refreshKeys() {
        if (keysJob?.isActive == true) return
        state.update { it.copy(keysLoading = true, keysError = null) }
        keysJob =
            viewModelScope.launch {
                when (val result = getKeys()) {
                    is Outcome.Success -> state.update { it.copy(keys = result.value, keysLoading = false, keysLoaded = true) }
                    is Outcome.Error -> state.update { it.copy(keysLoading = false, keysError = result.failure) }
                }
                reviewReady()
            }
    }

    private fun refreshAfterMutation() {
        profileJob?.cancel()
        keysJob?.cancel()
        refreshProfile()
        refreshKeys()
    }

    private fun reviewReady() {
        state.update {
            it.copy(
                reviewed =
                    when (saved.get<String>("pending")) {
                        "Name" -> !it.loading && it.profileError == null && it.profile != null
                        "Password" -> true // Passwords cannot be read back from the server.
                        else -> !it.keysLoading && it.keysError == null && it.keysLoaded
                    },
            )
        }
    }

    fun acknowledge() {
        if (!state.value.reviewed ||
            state.value.busy
        ) {
            return
        }
        saved.remove<String>("pending")
        saved.remove<Boolean>("tokenNeedsReview")
        state.update { it.copy(uncertain = false, passwordUncertain = false, message = null, error = null) }
    }

    fun edit(editor: AccountEditor) {
        if (state.value.busy || state.value.uncertain || state.value.editor == editor) return
        val name =
            state.value.profile
                ?.name
                .orEmpty()
        if (editor == AccountEditor.Name) {
            saved["editingName"] = true
            saved["name"] = name
        }
        state.update {
            it.copy(
                editor = editor,
                name =
                    if (editor ==
                        AccountEditor.Name
                    ) {
                        name
                    } else {
                        it.name
                    },
                passwords = PasswordFields(),
                error = null,
                message = null,
            )
        }
    }

    fun name(value: String) {
        saved["name"] = value
        state.update { it.copy(name = value, error = null) }
    }

    fun keyName(value: String) {
        saved["keyName"] = value
        state.update { it.copy(keyName = value, error = null) }
    }

    fun passwords(value: PasswordFields) {
        state.update { it.copy(passwords = value, error = null) }
    }

    fun closeEditor() {
        if (state.value.busy) return
        saved.remove<Boolean>("editingName")
        saved.remove<String>("name")
        saved.remove<String>("keyName")
        state.update { it.copy(editor = null, passwords = PasswordFields(), keyName = "", error = null) }
    }

    fun clearSecrets() {
        secretGeneration++
        saved.remove<Boolean>("tokenNeedsReview")
        state.update {
            it.copy(
                token = null,
                passwords = PasswordFields(),
                editor =
                    if (it.editor ==
                        AccountEditor.Password ||
                        it.editor == AccountEditor.Key
                    ) {
                        null
                    } else {
                        it.editor
                    },
            )
        }
    }

    fun clearToken() {
        saved.remove<Boolean>("tokenNeedsReview")
        state.update { it.copy(token = null) }
    }

    fun copied(success: Boolean) {
        state.update { it.copy(message = uiText(if (success) R.string.pulse_profile_copied else R.string.pulse_profile_copy_failed)) }
    }

    fun save() {
        val s = state.value
        if (s.busy || s.uncertain || s.editor == null) return
        val valid =
            when (s.editor) {
                AccountEditor.Name -> AccountValidation.validName(s.name)
                AccountEditor.Key -> AccountValidation.validName(s.keyName)
                AccountEditor.Password -> AccountValidation.validPassword(s.passwords.current, s.passwords.next, s.passwords.confirmation)
            }
        if (!valid) {
            state.update {
                it.copy(
                    error =
                        uiText(
                            if (s.editor ==
                                AccountEditor.Password
                            ) {
                                R.string.pulse_profile_password_validation
                            } else {
                                R.string.pulse_profile_name_validation
                            },
                        ),
                )
            }
            return
        }
        state.update { it.copy(busy = true, error = null) }
        saved["pending"] = s.editor.name
        val generation = secretGeneration
        viewModelScope.launch {
            val result: Outcome<*> =
                when (s.editor) {
                    AccountEditor.Name -> saveName(s.name)
                    AccountEditor.Password -> password(s.passwords.current, s.passwords.next, s.passwords.confirmation)
                    AccountEditor.Key -> createKey(s.keyName)
                }
            when (result) {
                is Outcome.Success -> {
                    if (s.editor == AccountEditor.Key) saved["tokenNeedsReview"] = true
                    val lostToken = s.editor == AccountEditor.Key && generation != secretGeneration
                    saved.remove<String>("pending")
                    saved.remove<Boolean>("editingName")
                    saved.remove<String>("name")
                    saved.remove<String>("keyName")
                    state.update {
                        it.copy(
                            busy = false,
                            uncertain = lostToken,
                            reviewed = false,
                            editor = null,
                            passwords = PasswordFields(),
                            keyName = "",
                            token =
                                if (generation ==
                                    secretGeneration
                                ) {
                                    result.value as? CreatedAPIKey
                                } else {
                                    null
                                },
                            profile = (result.value as? Profile) ?: it.profile,
                            message =
                                if (s.editor ==
                                    AccountEditor.Key
                                ) {
                                    if (generation ==
                                        secretGeneration
                                    ) {
                                        null
                                    } else {
                                        uiText(R.string.pulse_profile_uncertain)
                                    }
                                } else {
                                    uiText(
                                        if (s.editor ==
                                            AccountEditor.Password
                                        ) {
                                            R.string.pulse_profile_password_changed
                                        } else {
                                            R.string.pulse_profile_saved
                                        },
                                    )
                                },
                        )
                    }
                    refreshAfterMutation()
                }
                is Outcome.Error -> {
                    val uncertain = result.failure in setOf(Failure.Connection, Failure.Timeout, Failure.Server, Failure.Malformed)
                    if (!uncertain) saved.remove<String>("pending")
                    state.update {
                        it.copy(
                            busy = false,
                            uncertain = uncertain,
                            passwordUncertain = uncertain && s.editor == AccountEditor.Password,
                            reviewed = uncertain && s.editor == AccountEditor.Password,
                            editor = if (uncertain && s.editor == AccountEditor.Password) null else it.editor,
                            passwords = if (uncertain && s.editor == AccountEditor.Password) PasswordFields() else it.passwords,
                            error =
                                if (result.failure == Failure.InvalidCredentials &&
                                    s.editor == AccountEditor.Password
                                ) {
                                    uiText(R.string.pulse_profile_wrong_password)
                                } else {
                                    result.failure.message
                                },
                            message =
                                if (uncertain) {
                                    uiText(
                                        if (s.editor ==
                                            AccountEditor.Password
                                        ) {
                                            R.string.pulse_profile_password_uncertain
                                        } else if (s.editor == AccountEditor.Name) {
                                            R.string.pulse_profile_name_uncertain
                                        } else {
                                            R.string.pulse_profile_uncertain
                                        },
                                    )
                                } else {
                                    null
                                },
                        )
                    }
                    if (uncertain) {
                        refreshAfterMutation()
                    }
                }
            }
        }
    }

    fun revoke(id: Long) {
        if (state.value.busy || state.value.uncertain) return
        state.update { it.copy(busy = true) }
        saved["pending"] = "revoke"
        viewModelScope.launch {
            when (val result = revokeKey(id)) {
                is Outcome.Success -> {
                    saved.remove<String>("pending")
                    state.update {
                        it.copy(
                            busy = false,
                            keys =
                                it.keys.filterNot { key ->
                                    key.id ==
                                        id
                                },
                            message = uiText(R.string.pulse_profile_revoked),
                        )
                    }
                    refreshAfterMutation()
                }
                is Outcome.Error -> {
                    val uncertain =
                        result.failure in setOf(Failure.Connection, Failure.Timeout, Failure.Server, Failure.Malformed)
                    if (!uncertain) saved.remove<String>("pending")
                    state.update {
                        it.copy(
                            busy = false,
                            uncertain = uncertain,
                            reviewed = false,
                            message = if (uncertain) uiText(R.string.pulse_profile_uncertain) else result.failure.message,
                        )
                    }
                    if (uncertain) {
                        refreshAfterMutation()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        clearSecrets()
    }
}
