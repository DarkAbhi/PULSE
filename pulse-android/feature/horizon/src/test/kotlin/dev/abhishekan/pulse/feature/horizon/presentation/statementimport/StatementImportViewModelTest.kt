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
            vm.reset()
            runCurrent()
            vm.select(sampleAccount)
            vm.upload { byteArrayOf(1) }
            runCurrent()
            assertFalse(vm.uiState.value.confirmed)
            vm.commit()
            runCurrent()
            assertNull(repo.committedRevision)

            vm.confirmAccount(true)
            vm.edit(repo.row.copy(included = false))
            vm.commit()
            runCurrent()

            assertEquals(2, repo.committedRevision)
            assertEquals(false, repo.editedRow?.included)
            assertEquals(StatementCommit(0, 0, 1), vm.uiState.value.result)
        }

    @Test fun aReconciliationFailureNeverBecomesAReadyPreview() =
        runTest {
            repo.uploadFailure = "RECONCILIATION_FAILED"
            val vm = StatementImportViewModel(accounts, repo)
            vm.select(sampleAccount)
            vm.upload { byteArrayOf(1) }
            runCurrent()
            assertEquals("RECONCILIATION_FAILED", vm.uiState.value.errorCode)
            assertNull(vm.uiState.value.batch)
            vm.commit()
            runCurrent()
            assertNull(repo.committedRevision)
        }

    private class ImportRepository : StatementImportRepository {
        val row = StatementRow(1, "2026-01-02", "Test transaction", "10.00", "debit", null, true, false)
        private val batch = StatementBatch("test", "INR", 1, false, true, 1, 0)
        private val rows = StatementRows(listOf(row), 1, 100, 1)
        var uploadFailure: String? = null
        var editedRow: StatementRow? = null
        var committedRevision: Int? = null

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
            committedRevision = batch.revision
            return StatementResponse.Success(StatementCommit(0, 0, 1))
        }

        override suspend fun cancelStatement(id: String) = StatementResponse.Success(Unit)
    }
}
