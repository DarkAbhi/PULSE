package dev.abhishekan.pulse.feature.horizon.presentation.statementimport

import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.StatementBatch
import dev.abhishekan.pulse.core.domain.model.StatementCommit
import dev.abhishekan.pulse.core.domain.model.StatementPreview
import dev.abhishekan.pulse.core.domain.model.StatementResponse
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.domain.model.StatementRows
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
import dev.abhishekan.pulse.core.domain.repository.StatementImportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatementImportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = ImportRepository()
    private val sampleAccount = FinancialAccount(id = 7, currency = "INR")
    private val accounts =
        object : FinancialAccountsRepository {
            override suspend fun accounts(includeArchived: Boolean) = Outcome.Success(listOf(sampleAccount))

            override suspend fun saveAccount(account: FinancialAccount) = Outcome.Success(Unit)

            override suspend fun archiveAccount(
                id: Long,
                archived: Boolean,
            ) = Outcome.Success(Unit)
        }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun requiresDestinationConfirmationAndCommitsTheUpdatedRevision() =
        runTest {
            val vm = StatementImportViewModel(accounts, repo)
            vm.onAction(StatementImportAction.Reset)
            runCurrent()
            vm.onAction(StatementImportAction.SelectAccount(sampleAccount))
            vm.onAction(StatementImportAction.Upload { byteArrayOf(1) })
            runCurrent()
            assertFalse(vm.uiState.value.confirmed)
            vm.onAction(StatementImportAction.Commit)
            runCurrent()
            assertNull(repo.committedRevision)

            vm.onAction(StatementImportAction.ConfirmAccount(true))
            vm.onAction(StatementImportAction.EditRow(repo.row.copy(included = false)))
            vm.onAction(StatementImportAction.Commit)
            runCurrent()

            assertEquals(2, repo.committedRevision)
            assertEquals(false, repo.editedRow?.included)
            val completed = StatementImportEvent.Completed(StatementCommit(0, 0, 1))
            assertEquals(StatementImportExit.Pending(completed), vm.uiState.first().exit)
            vm.onAction(StatementImportAction.ExitHandled(completed))
            vm.onAction(StatementImportAction.Commit)
            runCurrent()
            assertEquals(StatementImportExit.Handled, vm.uiState.first().exit)
            assertEquals(1, repo.commitCount)
        }

    @Test fun aReconciliationFailureNeverBecomesAReadyPreview() =
        runTest {
            repo.uploadFailure = "RECONCILIATION_FAILED"
            val vm = StatementImportViewModel(accounts, repo)
            vm.onAction(StatementImportAction.SelectAccount(sampleAccount))
            vm.onAction(StatementImportAction.Upload { byteArrayOf(1) })
            runCurrent()
            assertEquals("RECONCILIATION_FAILED", vm.uiState.value.errorCode)
            assertNull(vm.uiState.value.batch)
            vm.onAction(StatementImportAction.Commit)
            runCurrent()
            assertNull(repo.committedRevision)
        }

    @Test fun closeWaitsForCancellationAndCanBeHandledAfterTheScreenReattaches() =
        runTest {
            val vm = StatementImportViewModel(accounts, repo)
            vm.onAction(StatementImportAction.SelectAccount(sampleAccount))
            vm.onAction(StatementImportAction.Upload { byteArrayOf(1) })
            runCurrent()

            repo.cancelFailure = "EXTRACTOR_BUSY"
            vm.onAction(StatementImportAction.Close)
            runCurrent()
            assertNull(vm.uiState.value.exit)
            assertEquals("EXTRACTOR_BUSY", vm.uiState.value.errorCode)

            repo.cancelFailure = null
            vm.onAction(StatementImportAction.Close)
            vm.onAction(StatementImportAction.Close)
            runCurrent()
            assertEquals(StatementImportExit.Pending(StatementImportEvent.Closed), vm.uiState.first().exit)
            vm.onAction(StatementImportAction.ExitHandled(StatementImportEvent.Closed))
            assertEquals(StatementImportExit.Handled, vm.uiState.first().exit)
            assertEquals(2, repo.cancelCount)

            vm.onAction(StatementImportAction.Reset)
            runCurrent()
            assertNull(vm.uiState.value.exit)
            assertEquals(listOf(sampleAccount), vm.uiState.value.accounts)
        }

    private class ImportRepository : StatementImportRepository {
        val row = StatementRow(1, "2026-01-02", "Test transaction", "10.00", "debit", null, true, false)
        private val batch = StatementBatch("test", "INR", 1, false, true, 1, 0)
        private val rows = StatementRows(listOf(row), 1, 100, 1)
        var uploadFailure: String? = null
        var cancelFailure: String? = null
        var editedRow: StatementRow? = null
        var committedRevision: Int? = null
        var commitCount = 0
        var cancelCount = 0

        override suspend fun uploadStatement(
            accountId: Long,
            pdf: ByteArray,
            progress: (Float?) -> Unit,
        ): StatementResponse<StatementPreview> {
            progress(1f)
            return uploadFailure?.let { StatementResponse.Rejected(it) } ?: StatementResponse.Success(StatementPreview(batch, rows))
        }

        override suspend fun statementBatch(id: String) = StatementResponse.Success(batch)

        override suspend fun statementRows(
            id: String,
            page: Int,
        ) = StatementResponse.Success(rows)

        override suspend fun editStatement(
            batch: StatementBatch,
            confirmed: Boolean,
            changes: List<Pair<StatementRow, StatementRow>>,
        ): StatementResponse<StatementBatch> {
            editedRow = changes.single().second
            return StatementResponse.Success(batch.copy(revision = 2, accountConfirmationRequired = !confirmed))
        }

        override suspend fun commitStatement(batch: StatementBatch): StatementResponse<StatementCommit> {
            commitCount++
            committedRevision = batch.revision
            return StatementResponse.Success(StatementCommit(0, 0, 1))
        }

        override suspend fun cancelStatement(id: String): StatementResponse<Unit> {
            cancelCount++
            val failure = cancelFailure
            return if (failure == null) StatementResponse.Success(Unit) else StatementResponse.Rejected(failure)
        }
    }
}
