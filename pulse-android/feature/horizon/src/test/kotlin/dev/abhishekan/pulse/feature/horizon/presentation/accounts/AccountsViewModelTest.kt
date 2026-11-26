package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repository = FakeAccountsRepository()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test fun fieldActionsUpdateTheLatestDraftAndOnlyValidDraftsAreSaved() =
        runTest {
            val vm = viewModel()
            runCurrent()

            vm.onAction(AccountsAction.Add(AccountType.CreditCard))
            vm.onAction(AccountsAction.ChangeName("Everyday"))
            vm.onAction(AccountsAction.ChangeInstitution("Sample Bank"))
            vm.onAction(AccountsAction.ChangeHolderName("A Person"))
            vm.onAction(AccountsAction.ChangeCardType("Millennia"))
            vm.onAction(AccountsAction.SelectNetwork("visa"))
            vm.onAction(AccountsAction.ChangeCreditLimit("200000.50"))
            vm.onAction(AccountsAction.ChangeStatementDay("31"))
            vm.onAction(AccountsAction.SelectSubtype("savings"))
            vm.onAction(AccountsAction.ChangeLastFour("12"))
            vm.onAction(AccountsAction.Save)
            runCurrent()
            assertFalse(vm.uiState.value.canSave)
            assertTrue(repository.saved.isEmpty())

            vm.onAction(AccountsAction.ChangeLastFour("0123"))
            vm.onAction(AccountsAction.ChangeLastFour("abcd"))
            vm.onAction(AccountsAction.Save)
            vm.onAction(AccountsAction.Save)
            vm.onAction(AccountsAction.Cancel)
            assertTrue(vm.uiState.value.busy)
            val draft = vm.uiState.value.draft
            assertEquals("Everyday", draft?.displayName)
            runCurrent()

            assertEquals(
                listOf(
                    FinancialAccount(
                        displayName = "Everyday",
                        institutionName = "Sample Bank",
                        accountType = "credit_card",
                        lastFour = "0123",
                        holderName = "A Person",
                        cardType = "Millennia",
                        cardNetwork = "visa",
                        creditLimit = "200000.50",
                        statementDay = "31",
                    ),
                ),
                repository.saved,
            )
            assertNull(vm.uiState.value.draft)
            assertEquals(AccountsState.Request.Idle, vm.uiState.value.request)
        }

    @Test fun failedEditKeepsTheDraftAndFailedReloadDoesNotResubmitTheWrite() =
        runTest {
            val vm = viewModel()
            runCurrent()
            vm.onAction(AccountsAction.Edit(repository.account))
            vm.onAction(AccountsAction.ChangeName("Renamed"))
            repository.saveResult = Outcome.Error(Failure.Server)
            vm.onAction(AccountsAction.Save)
            runCurrent()

            assertEquals(AccountsState.Request.Failed(Failure.Server), vm.uiState.value.request)
            assertEquals(repository.account.copy(displayName = "Renamed"), vm.uiState.value.draft)
            assertTrue(vm.uiState.value.canSave)

            repository.saveResult = Outcome.Success(Unit)
            repository.readFailure = Failure.Connection
            vm.onAction(AccountsAction.Save)
            runCurrent()
            assertNull(vm.uiState.value.draft)
            assertEquals(listOf(repository.account), vm.uiState.value.accounts)
            assertEquals(AccountsState.Request.Failed(Failure.Connection), vm.uiState.value.request)

            repository.readFailure = null
            vm.onAction(AccountsAction.Refresh)
            runCurrent()
            assertEquals(2, repository.saved.size)
            assertEquals(listOf(repository.account.copy(displayName = "Renamed")), vm.uiState.value.accounts)
            assertEquals(AccountsState.Request.Idle, vm.uiState.value.request)
        }

    @Test fun archiveAndRestoreRefreshTheSharedListWithArchivedAccountsIncluded() =
        runTest {
            val vm = viewModel()
            assertEquals(AccountsState.Request.Loading, vm.uiState.value.request)
            runCurrent()
            vm.onAction(AccountsAction.SetArchived(1, true))
            runCurrent()
            assertEquals(listOf(repository.account.copy(archived = true)), vm.uiState.value.accounts)
            vm.onAction(AccountsAction.SetArchived(1, false))
            runCurrent()
            assertEquals(listOf(repository.account), vm.uiState.value.accounts)
            assertEquals(listOf(true, true, true), repository.includeArchived)
        }

    @Test fun changingAnExistingAccountTypeClearsTheBankSubtype() =
        runTest {
            val vm = viewModel()
            runCurrent()
            val bank = repository.account.copy(bankAccountSubtype = "savings")
            vm.onAction(AccountsAction.Edit(bank))

            vm.onAction(AccountsAction.SelectType(AccountType.Bank))
            assertEquals(bank, vm.uiState.value.draft)

            vm.onAction(AccountsAction.SelectType(AccountType.CreditCard))
            assertEquals(bank.copy(accountType = "credit_card", bankAccountSubtype = "", holderEmail = ""), vm.uiState.value.draft)
        }

    @Test fun bankNumberIsOptionalAndCardLimitsAndStatementDaysAreValidated() {
        val bank = repository.account
        assertTrue(AccountsState(draft = bank).canSave)
        assertTrue(AccountsState(draft = bank.copy(accountNumber = "001234")).canSave)
        assertFalse(AccountsState(draft = bank.copy(holderEmail = "invalid")).canSave)
        val card =
            bank.copy(
                accountType = "credit_card",
                bankAccountSubtype = "",
                holderEmail = "",
                cardType = "Millennia",
                cardNetwork = "visa",
                lastFour = "0123",
                creditLimit = "100000.50",
                statementDay = "31",
            )
        assertTrue(AccountsState(draft = card).canSave)
        assertFalse(AccountsState(draft = card.copy(creditLimit = "-1")).canSave)
        assertFalse(AccountsState(draft = card.copy(statementDay = "32")).canSave)
        assertFalse(AccountsState(draft = card.copy(statementDay = "0")).canSave)
        assertFalse(AccountsState(draft = card.copy(cardNetwork = "other")).canSave)
    }

    @Test fun accountListsFilterByTypeAndIncludeArchivedRecords() {
        val bank = repository.account
        val card = bank.copy(id = 2L, accountType = "credit_card", archived = true)
        val state = AccountsState(accounts = listOf(bank, card))

        assertEquals(listOf(bank), state.accountsOfType(AccountType.Bank))
        assertEquals(listOf(card), state.accountsOfType(AccountType.CreditCard))
        assertEquals(emptyList<FinancialAccount>(), state.copy(accounts = listOf(bank)).accountsOfType(AccountType.CreditCard))
        assertNull(AccountsState().accountsOfType(AccountType.Bank))
    }

    private fun viewModel() = AccountsViewModel(repository).also { models.put("accounts", it) }

    private class FakeAccountsRepository : FinancialAccountsRepository {
        val account =
            FinancialAccount(
                id = 1L,
                displayName = "Original",
                institutionName = "Sample Bank",
                bankAccountSubtype = "savings",
                holderName = "A Person",
                holderEmail = "person@example.com",
            )
        val includeArchived = mutableListOf<Boolean>()
        val saved = mutableListOf<FinancialAccount>()
        var saveResult: Outcome<Unit> = Outcome.Success(Unit)
        var readFailure: Failure? = null
        private var current = account

        override suspend fun accounts(includeArchived: Boolean): Outcome<List<FinancialAccount>> {
            this.includeArchived += includeArchived
            return readFailure?.let { Outcome.Error(it) } ?: Outcome.Success(listOf(current))
        }

        override suspend fun saveAccount(account: FinancialAccount): Outcome<Unit> {
            saved += account
            if (saveResult is Outcome.Success) current = account.copy(id = account.id ?: 2L)
            return saveResult
        }

        override suspend fun archiveAccount(
            id: Long,
            archived: Boolean,
        ): Outcome<Unit> {
            assertEquals(current.id, id)
            current = current.copy(archived = archived)
            return Outcome.Success(Unit)
        }
    }
}
