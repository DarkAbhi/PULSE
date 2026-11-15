package dev.abhishekan.pulse.feature.horizon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.StatementBatch
import dev.abhishekan.pulse.core.domain.model.StatementCommit
import dev.abhishekan.pulse.core.domain.model.StatementResponse
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.domain.model.StatementRows
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
import dev.abhishekan.pulse.core.domain.repository.StatementImportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class StatementImportState(
    val accounts: List<FinancialAccount>? = null,
    val account: FinancialAccount? = null,
    val batch: StatementBatch? = null,
    val page: StatementRows? = null,
    val edits: Map<Int, StatementRow> = emptyMap(),
    val result: StatementCommit? = null,
    val confirmed: Boolean = false,
    val busy: Boolean = false,
    val uploading: Boolean = false,
    val progress: Float? = null,
    val errorCode: String? = null,
    val failure: Failure? = null,
)

internal class StatementImportViewModel(
    private val accounts: FinancialAccountsRepository,
    private val imports: StatementImportRepository,
) : ViewModel() {
    private val state = MutableStateFlow(StatementImportState())
    val uiState = state.asStateFlow()

    fun reset() {
        state.value = StatementImportState()
        loadAccounts()
    }

    fun loadAccounts() =
        work {
            when (val result = accounts.accounts()) {
                is Outcome.Success -> state.update { it.copy(accounts = result.value) }
                is Outcome.Error -> state.update { it.copy(failure = result.failure) }
            }
        }

    fun select(account: FinancialAccount) = state.update { it.copy(account = account) }

    fun confirmAccount(confirmed: Boolean) = state.update { it.copy(confirmed = confirmed) }

    fun edit(row: StatementRow) = state.update { it.copy(edits = it.edits + (row.id to row)) }

    fun upload(readFile: suspend () -> ByteArray?) =
        work {
            val accountId = state.value.account?.id ?: return@work
            val pdf = readFile()
            if (pdf == null) {
                state.update { it.copy(errorCode = "INVALID_FILE") }
                return@work
            }
            state.update { it.copy(uploading = true, progress = null) }
            val response =
                imports.uploadStatement(accountId, pdf) { progress ->
                    state.update { it.copy(progress = progress) }
                }
            if (response is StatementResponse.Success) {
                val draft = response.value
                state.update {
                    it.copy(batch = draft.batch, page = draft.rows, confirmed = !draft.batch.accountConfirmationRequired)
                }
            } else {
                record(response)
            }
        }

    fun page(target: Int) =
        work {
            val current = saveEdits() ?: return@work
            loadPage(current.id, target)
        }

    fun reload() =
        work {
            val current = state.value.batch ?: return@work
            val response = imports.statementBatch(current.id)
            if (response is StatementResponse.Success) {
                state.update { it.copy(batch = response.value, confirmed = !response.value.accountConfirmationRequired) }
                loadPage(current.id, state.value.page?.page ?: 1)
            } else {
                record(response)
            }
        }

    fun commit() =
        work {
            if (!state.value.confirmed) return@work
            val current = saveEdits() ?: return@work
            val response = imports.commitStatement(current)
            if (response is StatementResponse.Success) {
                state.update { it.copy(result = response.value) }
            } else {
                record(response)
            }
        }

    fun close(dismiss: () -> Unit) {
        val current = state.value
        work {
            if (current.batch == null || current.result != null || current.errorCode == "BATCH_EXPIRED") {
                dismiss()
            } else {
                when (val response = imports.cancelStatement(current.batch.id)) {
                    is StatementResponse.Success -> dismiss()
                    else -> record(response)
                }
            }
        }
    }

    private suspend fun loadPage(
        id: String,
        target: Int,
    ) {
        val response = imports.statementRows(id, target)
        if (response is StatementResponse.Success) {
            state.update { it.copy(page = response.value, edits = emptyMap()) }
        } else {
            record(response)
        }
    }

    private suspend fun saveEdits(): StatementBatch? {
        val current = state.value
        val batch = current.batch ?: return null
        val changes =
            current.page?.rows.orEmpty().mapNotNull { original ->
                current.edits[original.id]?.takeUnless { it == original }?.let { original to it }
            }
        if (changes.isEmpty() && !(current.confirmed && batch.accountConfirmationRequired)) return batch
        val response = imports.editStatement(batch, current.confirmed, changes)
        if (response is StatementResponse.Success) {
            val updatedRows =
                current.page?.let { page ->
                    page.copy(rows = page.rows.map { current.edits[it.id] ?: it })
                }
            state.update { it.copy(batch = response.value, page = updatedRows, edits = emptyMap()) }
            return response.value
        }
        record(response)
        return null
    }

    private fun record(response: StatementResponse<*>) {
        when (response) {
            is StatementResponse.Rejected -> state.update { it.copy(errorCode = response.code) }
            is StatementResponse.Failed -> state.update { it.copy(failure = response.failure) }
            is StatementResponse.Success -> Unit
        }
    }

    private fun work(action: suspend () -> Unit) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, errorCode = null, failure = null) }
        viewModelScope.launch {
            try {
                action()
            } finally {
                state.update { it.copy(busy = false, uploading = false) }
            }
        }
    }
}
