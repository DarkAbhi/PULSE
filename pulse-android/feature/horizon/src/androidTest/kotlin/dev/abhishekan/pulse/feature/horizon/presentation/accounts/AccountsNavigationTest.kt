package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.HorizonTransactionPage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.domain.repository.FinancialAccountsRepository
import dev.abhishekan.pulse.core.domain.repository.HorizonRepository
import dev.abhishekan.pulse.core.domain.usecase.ApplyHorizonChange
import dev.abhishekan.pulse.core.domain.usecase.GetHorizonTransactions
import dev.abhishekan.pulse.core.domain.usecase.GetSubscriptionPayments
import dev.abhishekan.pulse.core.ui.PulseScreenTransition
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.HorizonDestination
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.HorizonHost
import dev.abhishekan.pulse.feature.horizon.presentation.navigation.destination
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class AccountsNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val models = ViewModelStore()

    @After fun cleanup() {
        compose.runOnIdle { models.clear() }
        stopKoin()
    }

    @Test
    fun independentListsOpenThroughHorizonAndReturnToAccounts() {
        val archivedIds = mutableSetOf<Long>()
        val repository =
            object : FinancialAccountsRepository {
                override suspend fun accounts(includeArchived: Boolean): Outcome<List<FinancialAccount>> =
                    Outcome.Success(
                        listOf(
                            FinancialAccount(id = 1, displayName = "Savings fixture", accountType = "bank_account"),
                            FinancialAccount(id = 2, displayName = "Card fixture", accountType = "credit_card"),
                        ).map { it.copy(archived = it.id in archivedIds) },
                    )

                override suspend fun saveAccount(account: FinancialAccount): Outcome<Unit> = Outcome.Success(Unit)

                override suspend fun archiveAccount(
                    id: Long,
                    archived: Boolean,
                ): Outcome<Unit> {
                    if (archived) archivedIds.add(id) else archivedIds.remove(id)
                    return Outcome.Success(Unit)
                }
            }
        startKoin {
            modules(
                module {
                    single<FinancialAccountsRepository> { repository }
                    viewModelOf(::AccountsViewModel)
                },
            )
        }
        lateinit var vm: HorizonViewModel
        compose.runOnIdle {
            val horizon = FakeHorizonRepository()
            vm =
                HorizonViewModel(
                    horizon,
                    GetHorizonTransactions(horizon),
                    GetSubscriptionPayments(horizon),
                    ApplyHorizonChange(horizon),
                    SavedStateHandle(mapOf("tab" to "Accounts")),
                )
            models.put("horizon", vm)
        }
        compose.setContent {
            val state by vm.uiState.collectAsStateWithLifecycle()
            val scrollStates = rememberSaveableStateHolder()
            PulseTheme {
                PulseScreenTransition(state, screenDepth = { it.destination.depth }) { screen, active ->
                    HorizonHost(screen, scrollStates, active, vm::onAction)
                }
            }
        }
        listOf(
            Triple("Bank Accounts", HorizonDestination.BankAccounts, "Savings fixture"),
            Triple("Credit Cards", HorizonDestination.CreditCards, "Card fixture"),
        ).forEach { (title, destination, name) ->
            compose.onNodeWithText(title).performClick()
            compose.runOnIdle { assertEquals(destination, vm.uiState.value.destination) }
            compose.onNodeWithText(name).assertIsDisplayed()
            compose.onNodeWithText(if (name == "Card fixture") "Savings fixture" else "Card fixture").assertDoesNotExist()
            val addTitle = if (destination == HorizonDestination.BankAccounts) "Add Bank Account" else "Add Credit Card"
            compose.onNodeWithContentDescription(addTitle).performClick()
            compose.onNodeWithText(addTitle).assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to $title").performClick()
            compose.onNodeWithText(name).assertIsDisplayed()
            compose.onNodeWithContentDescription("Actions for $name").performClick()
            compose.onNodeWithText("Edit account").performClick()
            compose.onNodeWithText(addTitle.replace("Add", "Edit")).assertIsDisplayed()
            compose.onNodeWithContentDescription("Back to $title").performClick()
            compose.onNodeWithContentDescription("Actions for $name").performClick()
            compose.onNodeWithText("Archive").performClick()
            compose.onNodeWithText("Archived").assertIsDisplayed()
            compose.onNodeWithContentDescription("Actions for $name").performClick()
            compose.onNodeWithText("Restore").performClick()
            compose.onNodeWithText("Archived").assertDoesNotExist()
            compose.onNodeWithContentDescription("Back to Accounts").performClick()
            compose.runOnIdle { assertEquals(HorizonDestination.Tabs(HorizonTab.Accounts), vm.uiState.value.destination) }
        }
    }

    private class FakeHorizonRepository : HorizonRepository {
        override val changes = MutableStateFlow(0L)

        override suspend fun overview(): Outcome<HorizonOverview> = Outcome.Error(Failure.Connection)

        override suspend fun transactions(
            page: Int,
            search: String,
            type: String?,
            categoryId: Long?,
            pageSize: Int,
        ): Outcome<HorizonTransactionPage> = Outcome.Error(Failure.Connection)

        override suspend fun purchases(): Outcome<PlannedPurchases> = Outcome.Error(Failure.Connection)

        override suspend fun payments(subscriptionId: Long): Outcome<List<HorizonTransaction>> = Outcome.Success(emptyList())

        override suspend fun apply(change: HorizonChange): Outcome<Unit> = Outcome.Success(Unit)
    }
}
