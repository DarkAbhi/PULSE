package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class AccountsViewModel(
    private val repository: FinancialAccountsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountsState())
    val uiState = _uiState.asStateFlow()

    init {
        onAction(AccountsAction.Refresh)
    }

    fun onAction(action: AccountsAction) {
        if (_uiState.value.busy) return
        when (action) {
            AccountsAction.Refresh -> submit { load() }
            is AccountsAction.Add -> setDraft(FinancialAccount(currency = action.currency))
            is AccountsAction.Edit -> setDraft(action.account)
            AccountsAction.Cancel -> setDraft(null)
            is AccountsAction.ChangeName ->
                updateDraft { if (action.value.length <= 100) copy(displayName = action.value) else this }
            is AccountsAction.ChangeInstitution ->
                updateDraft { if (action.value.length <= 150) copy(institutionName = action.value) else this }
            is AccountsAction.SelectType ->
                updateDraft { copy(accountType = action.value, bankAccountSubtype = "") }
            is AccountsAction.SelectSubtype ->
                updateDraft { if (accountType == "bank_account") copy(bankAccountSubtype = action.value) else this }
            is AccountsAction.ChangeCountry ->
                updateDraft {
                    val value = action.value
                    if (value.length <= 2 && value.all { it.uppercaseChar() in 'A'..'Z' }) {
                        copy(countryCode = value.uppercase())
                    } else {
                        this
                    }
                }
            is AccountsAction.ChangeLastFour ->
                updateDraft {
                    val value = action.value
                    if (value.length <= 4 && value.all { it in '0'..'9' }) copy(lastFour = value) else this
                }
            AccountsAction.Save -> save()
            is AccountsAction.SetArchived -> setArchived(action.id, action.archived)
        }
    }

    private fun setDraft(account: FinancialAccount?) {
        _uiState.update { it.copy(draft = account, request = AccountsState.Request.Idle) }
    }

    private inline fun updateDraft(transform: FinancialAccount.() -> FinancialAccount) {
        _uiState.update { state ->
            val draft = state.draft ?: return@update state
            val updated = draft.transform()
            if (updated == draft) state else state.copy(draft = updated, request = AccountsState.Request.Idle)
        }
    }

    private fun save() {
        val state = _uiState.value
        val account = state.draft ?: return
        if (!state.canSave) return
        submit {
            when (val result = repository.saveAccount(account)) {
                is Outcome.Success -> {
                    _uiState.update { it.copy(draft = null) }
                    load()
                }
                is Outcome.Error -> fail(result.failure)
            }
        }
    }

    private fun setArchived(
        id: Long,
        archived: Boolean,
    ) = submit {
        when (val result = repository.archiveAccount(id, archived)) {
            is Outcome.Success -> load()
            is Outcome.Error -> fail(result.failure)
        }
    }

    private fun submit(operation: suspend () -> Unit) {
        _uiState.update { it.copy(request = AccountsState.Request.Loading) }
        viewModelScope.launch {
            try {
                operation()
            } finally {
                _uiState.update { if (it.busy) it.copy(request = AccountsState.Request.Idle) else it }
            }
        }
    }

    private suspend fun load() {
        when (val result = repository.accounts(includeArchived = true)) {
            is Outcome.Success -> _uiState.update { it.copy(accounts = result.value) }
            is Outcome.Error -> fail(result.failure)
        }
    }

    private fun fail(failure: Failure) {
        _uiState.update { it.copy(request = AccountsState.Request.Failed(failure)) }
    }
}

internal data class AccountsState(
    val accounts: List<FinancialAccount>? = null,
    val draft: FinancialAccount? = null,
    val request: Request = Request.Idle,
) {
    val busy get() = request == Request.Loading

    val canSave
        get() =
            !busy &&
                draft != null &&
                draft.displayName.isNotBlank() &&
                draft.institutionName.isNotBlank() &&
                (draft.countryCode.isEmpty() || draft.countryCode.length == 2) &&
                (draft.lastFour.isEmpty() || draft.lastFour.length == 4)

    sealed interface Request {
        data object Idle : Request

        data object Loading : Request

        data class Failed(
            val failure: Failure,
        ) : Request
    }
}

internal sealed interface AccountsAction {
    data object Refresh : AccountsAction

    data class Add(
        val currency: String,
    ) : AccountsAction

    data class Edit(
        val account: FinancialAccount,
    ) : AccountsAction

    data object Cancel : AccountsAction

    data class ChangeName(
        val value: String,
    ) : AccountsAction

    data class ChangeInstitution(
        val value: String,
    ) : AccountsAction

    data class SelectType(
        val value: String,
    ) : AccountsAction

    data class SelectSubtype(
        val value: String,
    ) : AccountsAction

    data class ChangeCountry(
        val value: String,
    ) : AccountsAction

    data class ChangeLastFour(
        val value: String,
    ) : AccountsAction

    data object Save : AccountsAction

    data class SetArchived(
        val id: Long,
        val archived: Boolean,
    ) : AccountsAction
}
