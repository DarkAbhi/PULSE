package dev.abhishekan.pulse.feature.horizon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonRecord
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetPlannedPurchases
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.core.domain.usecase.LoadHorizon
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.currencyText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
            LoadHorizon(repo),
            GetHorizonTransactions(repo),
            GetPlannedPurchases(repo),
            GetSubscriptionPayments(repo),
            ApplyHorizonChange(repo),
            saved,
        ).also {
            models.put("finance", it)
        }

    @Test fun refreshRetainsSectionsAndFailedRefreshKeepsDataWithError() =
        runTest {
            val vm = vm()
            runCurrent()
            val previous = vm.uiState.value
            repo.overviewFailure = Failure.Connection
            repo.listFailure = Failure.Connection
            repo.purchaseFailure = Failure.Connection
            vm.refresh()
            assertTrue(vm.uiState.value.overview.loading)
            assertEquals(previous.overview.value, vm.uiState.value.overview.value)
            assertEquals(previous.transactions.value, vm.uiState.value.transactions.value)
            assertEquals(previous.purchases.value, vm.uiState.value.purchases.value)
            runCurrent()
            assertFalse(vm.uiState.value.overview.loading)
            assertEquals(Failure.Connection, vm.uiState.value.overview.error)
            assertEquals(previous.overview.value, vm.uiState.value.overview.value)
            assertEquals(previous.transactions.value, vm.uiState.value.transactions.value)
            assertEquals(previous.purchases.value, vm.uiState.value.purchases.value)
        }

    @Test fun changingPageFilterOrSubscriptionDoesNotRetainUnrelatedRecords() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.refreshTransactions(2)
            assertNull(vm.uiState.value.transactions.value)
            runCurrent()
            vm.filter("tea", "debit")
            assertNull(vm.uiState.value.transactions.value)
            runCurrent()
            vm.showPayments(1)
            runCurrent()
            assertNotNull(vm.uiState.value.payments.value)
            vm.showPayments(2)
            assertNull(vm.uiState.value.payments.value)
            runCurrent()
        }

    @Test fun partialFailureKeepsOtherSectionsAndRetriesOnlyOneRead() =
        runTest {
            repo.overviewFailure = Failure.Server
            val vm = vm()
            runCurrent()
            assertEquals(Failure.Server, vm.uiState.value.overview.error)
            assertNotNull(vm.uiState.value.transactions.value)
            assertNotNull(vm.uiState.value.purchases.value)
            repo.overviewFailure = null
            vm.refreshOverview()
            runCurrent()
            assertEquals(1, repo.pages.size)
            assertNotNull(vm.uiState.value.overview.value)
        }

    @Test fun pagesAndFiltersAreServerDrivenAndSaved() =
        runTest {
            val saved = SavedStateHandle()
            val vm = vm(saved)
            runCurrent()
            vm.refreshTransactions(3)
            runCurrent()
            assertEquals(
                3,
                vm.uiState.value.transactions.value
                    ?.page,
            )
            vm.filter(" tea ", "debit")
            runCurrent()
            assertEquals(Triple(1, "tea", "debit"), repo.pages.last())
            vm.tab(HorizonTab.Transactions)
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
            assertEquals(2, repo.pages.last().first)
        }

    @Test fun failedPageRetryKeepsTheRequestedPage() =
        runTest {
            val vm = vm()
            runCurrent()
            repo.listFailure = Failure.Connection
            vm.refreshTransactions(3)
            runCurrent()
            assertEquals(Failure.Connection, vm.uiState.value.transactions.error)
            repo.listFailure = null
            vm.refreshTransactions()
            runCurrent()
            assertEquals(3, repo.pages.last().first)
            assertEquals(
                3,
                vm.uiState.value.transactions.value
                    ?.page,
            )
        }

    @Test fun deletionRefreshClosesPaymentsForAnAbsentSubscription() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.showPayments(4)
            runCurrent()
            vm.mutate(HorizonChange.Delete(HorizonRecord.Subscription, 4))
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
                vm.edit(it)
                vm.save()
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
            vm.edit(HorizonDraft(HorizonForm.Budget, name = "Food", amount = "12.50"))
            vm.save()
            vm.save()
            runCurrent()
            assertEquals(1, repo.writes.size)
            assertTrue(vm.uiState.value.busy)
            gate.complete(Outcome.Success(Unit))
            runCurrent()
            assertNull(vm.uiState.value.draft)
            assertFalse(vm.uiState.value.busy)
            assertEquals(2, repo.readCount)
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
            vm.edit(HorizonDraft(HorizonForm.Budget, name = "Food", amount = "12.50", dirty = true))
            vm.save()
            runCurrent()
            assertTrue(vm.uiState.value.reviewRequired)
            assertFalse(vm.uiState.value.reviewReady)
            vm.acknowledgeReview()
            vm.save()
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
            restored.refresh()
            runCurrent()
            restored.acknowledgeReview()
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
            vm.edit(draft)
            repo.overviewFailure = Failure.Connection
            val restored = vm(SavedStateHandle(mapOf("draft" to saved.get<ArrayList<String>>("draft"))))
            runCurrent()
            assertEquals(draft, restored.uiState.value.draft)
            restored.discard()
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
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.US)
            dev.abhishekan.pulse.core.domain.model.UserCurrency
                .select("USD")
            assertEquals(UiText.Literal("\$12.50"), currencyText("12.5".toBigDecimal()))
            assertEquals(UiText.Literal("-\$0.000000000000123456789"), currencyText("-0.000000000000123456789".toBigDecimal()))
        } finally {
            dev.abhishekan.pulse.core.domain.model.UserCurrency
                .reset()
            java.util.Locale.setDefault(previous)
        }
    }

    @Test fun allChangesValidateWithoutFloatingPointAndDeletionUsesTypedRecord() =
        runTest {
            val vm = vm()
            runCurrent()
            vm.mutate(HorizonChange.Delete(HorizonRecord.Budget, 3))
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
