package dev.abhishekan.pulse.feature.horizon

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
    private val state = MutableStateFlow(AccountsState())
    val uiState = state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = submit { load() }

    fun edit(account: FinancialAccount?) {
        state.update { it.copy(draft = account, error = null) }
    }

    fun save() {
        val account = state.value.draft ?: return
        submit {
            when (val result = repository.saveAccount(account)) {
                is Outcome.Success -> {
                    edit(null)
                    load()
                }
                is Outcome.Error -> state.update { it.copy(error = result.failure) }
            }
        }
    }

    fun archive(id: Long, archived: Boolean) = submit {
        when (val result = repository.archiveAccount(id, archived)) {
            is Outcome.Success -> load()
            is Outcome.Error -> state.update { it.copy(error = result.failure) }
        }
    }

    private fun submit(operation: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                operation()
            } finally {
                state.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun load() {
        when (val result = repository.accounts(includeArchived = true)) {
            is Outcome.Success -> state.update { it.copy(accounts = result.value, error = null) }
            is Outcome.Error -> state.update { it.copy(error = result.failure) }
        }
    }
}

internal data class AccountsState(
    val accounts: List<FinancialAccount>? = null,
    val draft: FinancialAccount? = null,
    val busy: Boolean = false,
    val error: Failure? = null,
)
