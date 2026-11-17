package dev.abhishekan.pulse.feature.horizon.presentation.state

import dev.abhishekan.pulse.core.domain.model.HorizonChange
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.feature.horizon.presentation.budgets.BudgetsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.ObligationsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.purchases.PlannedPurchasesTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionPaymentsAction
import dev.abhishekan.pulse.feature.horizon.presentation.subscriptions.SubscriptionsTabAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.history.TransactionHistoryAction
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.recent.RecentTransactionsAction

internal sealed interface HorizonAction {
    data object Refresh : HorizonAction

    data object RetryOverview : HorizonAction

    data object ConsumeMessage : HorizonAction

    data class SelectTab(
        val tab: HorizonTab,
    ) : HorizonAction

    data class Recent(
        val action: RecentTransactionsAction,
    ) : HorizonAction

    data class History(
        val action: TransactionHistoryAction,
    ) : HorizonAction

    data class Overview(
        val action: HorizonOverviewTabAction,
    ) : HorizonAction

    data class Budgets(
        val action: BudgetsTabAction,
    ) : HorizonAction

    data class Obligations(
        val action: ObligationsTabAction,
    ) : HorizonAction

    data class Subscriptions(
        val action: SubscriptionsTabAction,
    ) : HorizonAction

    data class Purchases(
        val action: PlannedPurchasesTabAction,
    ) : HorizonAction

    data class Payments(
        val action: SubscriptionPaymentsAction,
    ) : HorizonAction

    data object DismissTransaction : HorizonAction

    data object CloseImport : HorizonAction

    data object ManageAccounts : HorizonAction

    data object OpenBankAccounts : HorizonAction

    data object OpenCreditCards : HorizonAction

    data object ImportCompleted : HorizonAction

    data object Back : HorizonAction

    data object ToggleReviewRecords : HorizonAction

    data object ResumeDraft : HorizonAction

    data object AcknowledgeReview : HorizonAction

    data class Edit(
        val draft: HorizonDraft,
    ) : HorizonAction

    data class Editor(
        val action: HorizonEditorAction,
    ) : HorizonAction

    data class AskConfirmation(
        val change: HorizonChange,
        val message: UiText,
    ) : HorizonAction

    data object AcceptConfirmation : HorizonAction

    data object DismissConfirmation : HorizonAction
}
