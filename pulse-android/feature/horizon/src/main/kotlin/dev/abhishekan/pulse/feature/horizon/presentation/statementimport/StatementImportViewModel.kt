package dev.abhishekan.pulse.feature.horizon.presentation.statementimport

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
    val exit: StatementImportExit? = null,
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

    fun onAction(action: StatementImportAction) {
        if (state.value.busy && action !is StatementImportAction.ExitHandled) return
        if (state.value.exit != null && action != StatementImportAction.Reset && action !is StatementImportAction.ExitHandled) return

        when (action) {
            StatementImportAction.Reset -> reset()
            StatementImportAction.Entered -> {
                val current = state.value
                if (current.accounts == null && current.failure == null) loadAccounts()
            }
            StatementImportAction.RetryAccounts -> loadAccounts()
            is StatementImportAction.SelectAccount -> state.update { it.copy(account = action.account) }
            is StatementImportAction.ConfirmAccount -> state.update { it.copy(confirmed = action.confirmed) }
            is StatementImportAction.EditRow -> state.update { it.copy(edits = it.edits + (action.row.id to action.row)) }
            is StatementImportAction.Upload -> upload(action.readPdf)
            is StatementImportAction.Page -> page(action.target)
            StatementImportAction.Reload -> reload()
            StatementImportAction.Commit -> commit()
            StatementImportAction.Close -> close()
            StatementImportAction.ManageAccounts -> requestExit(StatementImportEvent.ManageAccounts)
            is StatementImportAction.ExitHandled ->
                state.update {
                    if (it.exit == StatementImportExit.Pending(action.event)) it.copy(exit = StatementImportExit.Handled) else it
                }
        }
    }

    private fun reset() {
        state.update { StatementImportState() }
        loadAccounts()
    }

    private fun loadAccounts() =
        work {
            when (val result = accounts.accounts()) {
                is Outcome.Success -> state.update { it.copy(accounts = result.value) }
                is Outcome.Error -> state.update { it.copy(failure = result.failure) }
            }
        }

    private fun upload(readFile: suspend () -> ByteArray?) =
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

    private fun page(target: Int) =
        work {
            val current = saveEdits() ?: return@work
            loadPage(current.id, target)
        }

    private fun reload() =
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

    private fun commit() =
        work {
            if (!state.value.confirmed) return@work
            val current = saveEdits() ?: return@work
            val response = imports.commitStatement(current)
            if (response is StatementResponse.Success) {
                requestExit(StatementImportEvent.Completed(response.value))
            } else {
                record(response)
            }
        }

    private fun close() {
        val current = state.value
        work {
            if (current.batch == null || current.errorCode == "BATCH_EXPIRED") {
                requestExit(StatementImportEvent.Closed)
            } else {
                when (val response = imports.cancelStatement(current.batch.id)) {
                    is StatementResponse.Success -> requestExit(StatementImportEvent.Closed)
                    else -> record(response)
                }
            }
        }
    }

    private fun requestExit(event: StatementImportEvent) {
        state.update { it.copy(exit = StatementImportExit.Pending(event)) }
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

internal sealed interface StatementImportAction {
    data object Reset : StatementImportAction

    data object Entered : StatementImportAction

    data object RetryAccounts : StatementImportAction

    data class SelectAccount(
        val account: FinancialAccount,
    ) : StatementImportAction

    data class ConfirmAccount(
        val confirmed: Boolean,
    ) : StatementImportAction

    data class EditRow(
        val row: StatementRow,
    ) : StatementImportAction

    data class Upload(
        val readPdf: suspend () -> ByteArray?,
    ) : StatementImportAction

    data class Page(
        val target: Int,
    ) : StatementImportAction

    data object Reload : StatementImportAction

    data object Commit : StatementImportAction

    data object Close : StatementImportAction

    data object ManageAccounts : StatementImportAction

    data class ExitHandled(
        val event: StatementImportEvent,
    ) : StatementImportAction
}

internal sealed interface StatementImportEvent {
    data object Closed : StatementImportEvent

    data object ManageAccounts : StatementImportEvent

    data class Completed(
        val result: StatementCommit,
    ) : StatementImportEvent
}

internal sealed interface StatementImportExit {
    data class Pending(
        val event: StatementImportEvent,
    ) : StatementImportExit

    data object Handled : StatementImportExit
}
