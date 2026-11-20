package dev.abhishekan.pulse.feature.horizon.presentation.navigation

import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountType
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonTab
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonUiState

internal sealed interface HorizonDestination {
    val depth: Int

    data class Tabs(
        val tab: HorizonTab,
    ) : HorizonDestination {
        override val depth = 0
    }

    data object History : HorizonDestination {
        override val depth = 1
    }

    data object BankAccounts : HorizonDestination {
        override val depth = 1
    }

    data object CreditCards : HorizonDestination {
        override val depth = 1
    }

    data class Editor(
        val draft: HorizonDraft,
        val parent: EditorParent,
    ) : HorizonDestination {
        override val depth = if (parent == EditorParent.Tab) 1 else 2
    }

    data object StatementImport : HorizonDestination {
        override val depth = 1
    }
}

internal enum class EditorParent(
    val backLabel: Int,
) {
    Tab(R.string.pulse_horizon_back_to_horizon),
    History(R.string.pulse_horizon_back_to_history),
    Transaction(R.string.pulse_horizon_back_to_transaction),
}

internal val HorizonUiState.destination: HorizonDestination
    get() =
        when {
            importOpen -> HorizonDestination.StatementImport
            accountList == AccountType.Bank -> HorizonDestination.BankAccounts
            accountList == AccountType.CreditCard -> HorizonDestination.CreditCards
            draft != null ->
                HorizonDestination.Editor(
                    draft,
                    when {
                        transactionDraft != null -> EditorParent.Transaction
                        transactionLists.historyOpen -> EditorParent.History
                        else -> EditorParent.Tab
                    },
                )
            else -> recordsDestination
        }

internal val HorizonUiState.recordsDestination: HorizonDestination
    get() = if (transactionLists.historyOpen) HorizonDestination.History else HorizonDestination.Tabs(tab)
