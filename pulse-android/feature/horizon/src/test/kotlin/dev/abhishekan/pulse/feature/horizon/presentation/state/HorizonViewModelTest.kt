package dev.abhishekan.pulse.feature.horizon.presentation.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.draft
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.EditorParent
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.HorizonDestination
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.destination
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionPaymentsAction
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.history.TransactionHistoryAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsAction
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class HorizonViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val repo = HorizonTestRepository()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun cleanup() {
        models.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun vm(saved: SavedStateHandle = SavedStateHandle()) =
        HorizonViewModel(
            repo,
            GetHorizonTransactions(repo),
            GetSubscriptionPayments(repo),
            ApplyHorizonChange(repo),
            saved,
        ).also {
            models.put("finance", it)
        }

    @Test fun accountListsUseHorizonDestinationsAndBackReturnsToAccounts() =
        runTest {
            var saved = SavedStateHandle()
            var model = vm(saved)
            runCurrent()
            listOf(
                HorizonAction.OpenBankAccounts to HorizonDestination.BankAccounts,
                HorizonAction.OpenCreditCards to HorizonDestination.CreditCards,
            ).forEach { (action, destination) ->
                model.onAction(HorizonAction.SelectTab(HorizonTab.Accounts))
                model.onAction(action)
                assertEquals(destination, model.uiState.value.destination)
                assertEquals(1, destination.depth)
                assertEquals(HorizonTab.Accounts, model.uiState.value.tab)
                saved = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
                model = vm(saved)
                runCurrent()
                assertEquals(destination, model.uiState.value.destination)
                model.onAction(HorizonAction.Back)
                assertEquals(HorizonDestination.Tabs(HorizonTab.Accounts), model.uiState.value.destination)
                assertNull(saved.get<String>("accountList"))
            }
        }

    @Test fun changingHorizonTabsClearsTheAccountListDestination() =
        runTest {
            val saved = SavedStateHandle()
            val model = vm(saved)
            runCurrent()
            model.onAction(HorizonAction.OpenCreditCards)
            model.onAction(HorizonAction.SelectTab(HorizonTab.Budgets))
            assertEquals(HorizonDestination.Tabs(HorizonTab.Budgets), model.uiState.value.destination)
            assertNull(saved.get<String>("accountList"))
        }

    @Test fun leavingAnAccountListPreservesTheHorizonDraftUnderneath() =
        runTest {
            val model = vm()
            runCurrent()
            val draft = HorizonDraft(HorizonForm.Budget, name = "Food", amount = "12.50", dirty = true)
            model.onAction(HorizonAction.Edit(draft))
            model.onAction(HorizonAction.ToggleReviewRecords)
            model.onAction(HorizonAction.OpenBankAccounts)
            assertEquals(HorizonDestination.BankAccounts, model.uiState.value.destination)
            model.onAction(HorizonAction.Back)
            assertEquals(draft, model.uiState.value.draft)
            assertNull(model.uiState.value.accountList)
            assertNull(model.uiState.value.confirmation)
        }

    @Test fun refreshRetainsSectionsAndFailedRefreshKeepsDataWithError() =
        runTest {
            val vm = vm()
            runCurrent()
            val previous = vm.uiState.value
            repo.overviewFailure = Failure.Connection
            repo.listFailure = Failure.Connection
            repo.purchaseFailure = Failure.Connection
            vm.onAction(HorizonAction.Refresh)
            assertTrue(vm.uiState.value.overview.loading)
            assertEquals(previous.overview.value, vm.uiState.value.overview.value)
            assertEquals(previous.transactionLists.recent.value, vm.uiState.value.transactionLists.recent.value)
            assertEquals(previous.purchases.value, vm.uiState.value.purchases.value)
            runCurrent()
            assertFalse(vm.uiState.value.overview.loading)
            assertEquals(Failure.Connection, vm.uiState.value.overview.error)
            assertEquals(previous.overview.value, vm.uiState.value.overview.value)
            assertEquals(previous.transactionLists.recent.value, vm.uiState.value.transactionLists.recent.value)
            assertEquals(previous.purchases.value, vm.uiState.value.purchases.value)
        }

    @Test fun changingPageFilterOrSubscriptionDoesNotRetainUnrelatedRecords() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.Page(2)))
            assertNull(vm.uiState.value.transactions.value)
            runCurrent()
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.Filter("tea", "debit")))
            assertNull(vm.uiState.value.transactions.value)
            runCurrent()
            vm.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.ViewPayments(1)))
            runCurrent()
            assertNotNull(vm.uiState.value.payments.value)
            vm.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.ViewPayments(2)))
            assertNull(vm.uiState.value.payments.value)
            runCurrent()
        }

    @Test fun partialFailureKeepsOtherSectionsAndRetriesOnlyOneRead() =
        runTest {
            repo.overviewFailure = Failure.Server
            val vm = vm()
            runCurrent()
            assertEquals(Failure.Server, vm.uiState.value.overview.error)
            assertNotNull(vm.uiState.value.transactionLists.recent.value)
            assertNotNull(vm.uiState.value.purchases.value)
            repo.overviewFailure = null
            vm.onAction(HorizonAction.RetryOverview)
            runCurrent()
            assertEquals(listOf(null to 5), repo.historyRequests)
            assertNotNull(vm.uiState.value.overview.value)
        }

    @Test fun pagesAndFiltersAreServerDrivenAndSaved() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.Page(3)))
            runCurrent()
            assertEquals(
                3,
                vm.uiState.value.transactions.value
                    ?.page,
            )
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.Filter(" tea ", "debit")))
            runCurrent()
            assertEquals(Triple(1, "tea", "debit"), repo.pages.last())
            vm.onAction(HorizonAction.SelectTab(HorizonTab.Transactions))
            val restored =
                vm(
                    SavedStateHandle(
                        mapOf(
                            "tab" to saved.get<String>("tab"),
                            "search" to saved.get<String>("search"),
                            "type" to saved.get<String>("type"),
                            "page" to 2,
                        ),
                    ),
                )
            runCurrent()
            assertEquals(HorizonTab.Transactions, restored.uiState.value.tab)
            restored.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.ViewPayments(1)))
            runCurrent()
            assertEquals(2, repo.pages.last().first)
        }

    @Test fun failedPageRetryKeepsTheRequestedPage() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.listFailure = Failure.Connection
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.Page(3)))
            runCurrent()
            assertEquals(Failure.Connection, vm.uiState.value.transactions.error)
            repo.listFailure = null
            vm.onAction(HorizonAction.Payments(SubscriptionPaymentsAction.RetryTransactions))
            runCurrent()
            assertEquals(3, repo.pages.last().first)
            assertEquals(
                3,
                vm.uiState.value.transactions.value
                    ?.page,
            )
        }

    @Test fun historyFiltersAndAppendingDoNotChangeTheRecentPreviewOrPaymentPicker() =
        runTest {
            val vm = vm()
            runCurrent()
            val recent = vm.uiState.value.transactionLists.recent.value
            vm.onAction(HorizonAction.Recent(RecentTransactionsAction.OpenHistory))
            runCurrent()
            vm.onAction(HorizonAction.History(TransactionHistoryAction.Filter("tea", "debit", 7)))
            runCurrent()
            assertEquals(7L to 20, repo.historyRequests.last())
            assertEquals(Triple(1, "tea", "debit"), repo.pages.last())
            repo.listFailure = Failure.Connection
            vm.onAction(HorizonAction.History(TransactionHistoryAction.LoadMore))
            runCurrent()
            assertEquals(
                1,
                vm.uiState.value.transactionLists.history.value
                    ?.page,
            )
            assertEquals(Failure.Connection, vm.uiState.value.transactionLists.history.error)
            repo.listFailure = null
            vm.onAction(HorizonAction.History(TransactionHistoryAction.Retry))
            runCurrent()
            assertEquals(
                2,
                vm.uiState.value.transactionLists.history.value
                    ?.page,
            )
            assertEquals(recent, vm.uiState.value.transactionLists.recent.value)
            vm.onAction(HorizonAction.History(TransactionHistoryAction.Back))
            vm.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.ViewPayments(1)))
            runCurrent()
            assertEquals(Triple(1, "", null), repo.pages.last())
            assertEquals(null to 20, repo.historyRequests.last())
        }

    @Test fun transactionSelectionRestoresForItsListAndClearsWhenEditingOrLeaving() =
        runTest {
            val saved = SavedStateHandle()
            val original = vm(saved)
            original.onAction(HorizonAction.SelectTab(HorizonTab.Transactions))
            original.onAction(HorizonAction.Recent(RecentTransactionsAction.Select(4)))
            assertEquals(TransactionSelection(4, TransactionList.Recent), original.uiState.value.transactionLists.selected)

            original.onAction(HorizonAction.Recent(RecentTransactionsAction.OpenHistory))
            assertNull(original.uiState.value.transactionLists.selected)
            original.onAction(HorizonAction.History(TransactionHistoryAction.Filter("tea", "debit", 7)))
            original.onAction(HorizonAction.History(TransactionHistoryAction.Select(4)))

            val restored = vm(saved)
            assertEquals(TransactionSelection(4, TransactionList.History), restored.uiState.value.transactionLists.selected)
            assertEquals(HistoryFilters("tea", "debit", 7), restored.uiState.value.transactionLists.historyFilters)
            restored.onAction(HorizonAction.Edit(HorizonDraft(HorizonForm.Transaction)))
            restored.onAction(HorizonAction.Editor(HorizonEditorAction.Cancel))
            assertNull(restored.uiState.value.transactionLists.selected)
            assertEquals(HorizonDestination.History, restored.uiState.value.destination)

            restored.onAction(HorizonAction.History(TransactionHistoryAction.Select(4)))
            restored.onAction(HorizonAction.Back)
            assertNull(restored.uiState.value.transactionLists.selected)
            assertEquals(HorizonDestination.Tabs(HorizonTab.Transactions), restored.uiState.value.destination)
        }

    @Test fun nestedCategorySaveAndCancelRestoreTheTransactionDraft() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            vm.onAction(HorizonAction.SelectTab(HorizonTab.Transactions))
            vm.onAction(HorizonAction.Recent(RecentTransactionsAction.OpenHistory))
            val transaction = HorizonDraft(HorizonForm.Transaction, name = "Tea", amount = "12.50", dirty = true)
            vm.onAction(HorizonAction.Edit(transaction))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.AddCategory))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Cancel))
            assertEquals(transaction, vm.uiState.value.draft)

            vm.onAction(HorizonAction.Editor(HorizonEditorAction.AddCategory))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Change(checkNotNull(vm.uiState.value.draft).copy(name = "Drinks"))))
            val restored = vm(saved)
            runCurrent()
            val categoryDestination = restored.uiState.value.destination as HorizonDestination.Editor
            assertEquals(HorizonForm.Category, categoryDestination.draft.kind)
            assertEquals(EditorParent.Transaction, categoryDestination.parent)
            restored.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            assertEquals(transaction, restored.uiState.value.draft)
            assertEquals(HorizonDestination.Editor(transaction, EditorParent.History), restored.uiState.value.destination)
            assertNull(restored.uiState.value.transactionDraft)
            assertEquals("Drinks", (repo.writes.single() as HorizonChange.Category).name)

            restored.onAction(HorizonAction.Edit(transaction.copy(id = 42)))
            restored.onAction(HorizonAction.Editor(HorizonEditorAction.AddCategory))
            assertEquals(
                HorizonForm.Transaction,
                restored.uiState.value.draft
                    ?.kind,
            )
        }

    @Test fun statementImportDestinationSurvivesRecreationAndReturnsToTransactions() =
        runTest {
            val saved = SavedStateHandle()
            val original = vm(saved)
            original.onAction(HorizonAction.SelectTab(HorizonTab.Transactions))
            original.onAction(HorizonAction.Recent(RecentTransactionsAction.Import))

            val restored = vm(saved)
            assertEquals(HorizonDestination.StatementImport, restored.uiState.value.destination)

            restored.onAction(HorizonAction.CloseImport)
            assertEquals(HorizonDestination.Tabs(HorizonTab.Transactions), restored.uiState.value.destination)

            restored.onAction(HorizonAction.Recent(RecentTransactionsAction.Import))
            runCurrent()
            val readsBeforeCompletion = repo.readCount
            restored.onAction(HorizonAction.ImportCompleted)
            restored.onAction(HorizonAction.ImportCompleted)
            runCurrent()
            assertEquals(HorizonDestination.Tabs(HorizonTab.Transactions), restored.uiState.value.destination)
            assertEquals(readsBeforeCompletion + 1, repo.readCount)
            assertNotNull(restored.uiState.value.message)
            restored.onAction(HorizonAction.ConsumeMessage)
            assertNull(restored.uiState.value.message)
        }

    @Test fun deletionRefreshClosesPaymentsForAnAbsentSubscription() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.ViewPayments(4)))
            runCurrent()
            vm.onAction(HorizonAction.Subscriptions(SubscriptionsTabAction.RequestDelete(4, "Subscription")))
            assertEquals(
                HorizonChange.Delete(HorizonRecord.Subscription, 4),
                (vm.uiState.value.confirmation as HorizonConfirmation.Apply).change,
            )
            vm.onAction(HorizonAction.AcceptConfirmation)
            runCurrent()
            assertNull(vm.uiState.value.subscriptionId)
            assertFalse(vm.uiState.value.payments.loading)
        }

    @Test fun invalidAmountsDatesAndDaysNeverReachRepository() =
        runTest {
            val vm = vm()
            runCurrent()
            listOf(
                HorizonDraft(HorizonForm.Transaction, name = "Bad", amount = "0", date = "2026-01-01"),
                HorizonDraft(HorizonForm.Budget, name = "Precision", amount = "1.001"),
                HorizonDraft(HorizonForm.Deduction, name = "Bad day", amount = "1", dueDay = "32"),
                HorizonDraft(HorizonForm.Transaction, name = "Date", amount = "1", date = "2026-02-30"),
            ).forEach {
                vm.onAction(HorizonAction.Edit(it))
                vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
                runCurrent()
                assertNotNull(vm.uiState.value.formError)
            }
            assertTrue(repo.writes.isEmpty())
        }

    @Test fun duplicateSavesAreBlockedAndSuccessfulSaveRefreshes() =
        runTest {
            val gate = CompletableDeferred<Outcome<Unit>>()
            repo.write = { gate.await() }
            val vm = vm()
            runCurrent()
            vm.onAction(HorizonAction.Edit(HorizonDraft(HorizonForm.Budget, name = "Food", amount = "12.50")))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            assertEquals(1, repo.writes.size)
            assertTrue(vm.uiState.value.busy)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertNull(vm.uiState.value.draft)
            assertFalse(vm.uiState.value.busy)
            assertEquals(2, repo.readCount)
        }

    @Test fun subscriptionBillingFieldsFollowTheSelectedCycle() =
        runTest {
            val vm = vm()
            runCurrent()
            val monthly =
                HorizonDraft(
                    HorizonForm.Subscription,
                    name = "Music",
                    amount = "299",
                    dueDay = "29",
                    date = "2028-02-20",
                )
            listOf("", "0", "32", "1.5").forEach { day ->
                vm.onAction(HorizonAction.Edit(monthly.copy(dueDay = day)))
                vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
                runCurrent()
                assertNotNull(vm.uiState.value.formError)
            }
            vm.onAction(HorizonAction.Edit(monthly.copy(cycle = "yearly", date = "")))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            assertNotNull(vm.uiState.value.formError)
            assertTrue(repo.writes.isEmpty())

            vm.onAction(HorizonAction.Edit(monthly))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            val monthlyChange = repo.writes.last() as HorizonChange.Subscription
            assertEquals(29, monthlyChange.billingDay)
            assertNull(monthlyChange.renewalDate)
            assertNull(monthlyChange.deductionId)

            vm.onAction(HorizonAction.Edit(monthly.copy(cycle = "yearly", dueDay = "invalid")))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            val yearlyChange = repo.writes.last() as HorizonChange.Subscription
            assertEquals(20, yearlyChange.billingDay)
            assertEquals(LocalDate.parse("2028-02-20"), yearlyChange.renewalDate)
        }

    @Test fun uncertainWriteRequiresFreshStateAndReviewEvenAfterProcessRestore() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            repo.write = {
                repo.listFailure = Failure.Connection
                Outcome.Error(Failure.Timeout)
            }
            vm.onAction(HorizonAction.Edit(HorizonDraft(HorizonForm.Budget, name = "Food", amount = "12.50", dirty = true)))
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            assertTrue(vm.uiState.value.reviewRequired)
            assertFalse(vm.uiState.value.reviewReady)
            vm.onAction(HorizonAction.AcknowledgeReview)
            vm.onAction(HorizonAction.Editor(HorizonEditorAction.Save))
            runCurrent()
            assertEquals(1, repo.writes.size)
            val restored =
                vm(
                    SavedStateHandle(
                        mapOf(
                            "pending" to true,
                            "draft" to saved.get<ArrayList<String>>("draft"),
                        ),
                    ),
                )
            runCurrent()
            assertEquals(
                "Food",
                restored.uiState.value.draft
                    ?.name,
            )
            assertTrue(restored.uiState.value.reviewRequired)
            repo.listFailure = null
            restored.onAction(HorizonAction.Refresh)
            runCurrent()
            restored.onAction(HorizonAction.AcknowledgeReview)
            assertFalse(restored.uiState.value.reviewRequired)
        }

    @Test fun offlineProcessRestorePreservesEveryDraftField() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            val draft =
                HorizonDraft(
                    HorizonForm.Subscription,
                    9,
                    "Music",
                    "150.25",
                    "USD",
                    "2028-02-29",
                    "10:20",
                    "credit",
                    "other",
                    "31",
                    false,
                    "yearly",
                    "paused",
                    1,
                    2,
                    3,
                    4,
                    "notes",
                    "url",
                    "tag",
                    "color",
                    true,
                )
            vm.onAction(HorizonAction.Edit(draft))
            repo.overviewFailure = Failure.Connection
            val restored = vm(SavedStateHandle(mapOf("draft" to saved.get<ArrayList<String>>("draft"))))
            runCurrent()
            assertEquals(draft, restored.uiState.value.draft)
            restored.onAction(HorizonAction.Editor(HorizonEditorAction.Cancel))
            assertEquals(HorizonConfirmation.Discard, restored.uiState.value.confirmation)
            restored.onAction(HorizonAction.AcceptConfirmation)
            assertNull(restored.uiState.value.draft)
        }

    @Test fun transactionEditingKeepsSecondsAndDecimalWithoutRounding() {
        val tx =
            HorizonTransaction(
                1,
                "Tea",
                "2.50".toBigDecimal(),
                "debit",
                Instant.parse(
                    "2026-10-17T12:34:56.123456Z",
                ),
                null,
                "Other",
                null,
                null,
                null,
                null,
                null,
            )
        val change = tx.draft("₹").change() as HorizonChange.Transaction
        assertEquals(tx.date, change.date)
        assertEquals(tx.amount, change.amount)
    }

    @Test fun monetaryDisplayUsesAccountCurrencyAndPreservesPrecision() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            UserCurrency
                .select("USD")
            assertEquals(UiText.Literal("\$12.50"), currencyText("12.5".toBigDecimal()))
            assertEquals(UiText.Literal("-\$0.000000000000123456789"), currencyText("-0.000000000000123456789".toBigDecimal()))
        } finally {
            UserCurrency
                .reset()
            Locale.setDefault(previous)
        }
    }

    @Test fun allChangesValidateWithoutFloatingPointAndDeletionUsesTypedRecord() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.onAction(HorizonAction.AskConfirmation(HorizonChange.Delete(HorizonRecord.Budget, 3), UiText.Literal("Confirm")))
            vm.onAction(HorizonAction.AcceptConfirmation)
            runCurrent()
            assertEquals(HorizonChange.Delete(HorizonRecord.Budget, 3), repo.writes.single())
            val apply = ApplyHorizonChange(repo)
            assertFalse(apply.valid(HorizonChange.Config("10000000000".toBigDecimal(), "₹")))
            assertFalse(
                apply.valid(
                    HorizonChange.Transaction(null, "Credit", "0".toBigDecimal(), "credit", Instant.now(), null, null, null, null),
                ),
            )
            assertTrue(
                apply.valid(HorizonChange.Budget(null, "Budget", "9999999999.99".toBigDecimal())),
            )
            assertTrue(
                apply.valid(
                    HorizonChange.Subscription(
                        null,
                        "Optional",
                        "0".toBigDecimal(),
                        "yearly",
                        null,
                        null,
                        "cancelled",
                        null,
                        null,
                        null,
                        null,
                    ),
                ),
            )
        }
}
