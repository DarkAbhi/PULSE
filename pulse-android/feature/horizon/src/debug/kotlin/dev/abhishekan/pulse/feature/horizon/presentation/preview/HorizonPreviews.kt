package dev.abhishekan.pulse.feature.horizon.presentation.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.Failure
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonProjection
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseFormLayout
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountEditorScreen
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountListScreen
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountType
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsState
import dev.abhishekan.pulse.feature.horizon.presentation.accounts.AccountsTabContent
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonTabScroll
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonForm
import dev.abhishekan.pulse.feature.horizon.presentation.obligations.FixedObligationEditor
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonMetrics
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabScreen
import dev.abhishekan.pulse.feature.horizon.presentation.overview.HorizonOverviewTabState
import dev.abhishekan.pulse.feature.horizon.presentation.overview.ProjectionCard
import dev.abhishekan.pulse.feature.horizon.presentation.state.HorizonSection
import dev.abhishekan.pulse.feature.horizon.presentation.transactions.editor.TransactionEditor
import dev.abhishekan.pulse.core.ui.R as CoreUiR

private val sample =
    HorizonOverview(
        "USD",
        "2400.00".toBigDecimal(),
        "875.50".toBigDecimal(),
        "1500.00".toBigDecimal(),
        "120.00".toBigDecimal(),
        "600.00".toBigDecimal(),
        "24.50".toBigDecimal(),
        "63.52".toBigDecimal(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        listOf(
            HorizonProjection(3, "3 Months", "300000.00".toBigDecimal()),
            HorizonProjection(6, "6 Months", "600000.00".toBigDecimal()),
            HorizonProjection(12, "12 Months", "1200000.00".toBigDecimal()),
        ),
    )

private val sampleAccount =
    FinancialAccount(
        id = 1L,
        displayName = "Everyday account",
        institutionName = "Sample Bank",
        bankAccountSubtype = "savings",
        lastFour = "4821",
    )

private val sampleCard =
    sampleAccount.copy(
        id = 2L,
        displayName = "Travel rewards",
        accountType = "credit_card",
        bankAccountSubtype = "",
        lastFour = "9062",
        archived = true,
    )

private val sampleAccounts =
    AccountsState(
        accounts =
            listOf(
                sampleAccount,
                sampleAccount.copy(id = 3L, displayName = "Kotak", institutionName = "Kotak Bank"),
                sampleAccount.copy(id = 4L, displayName = "SBI", institutionName = "State Bank of India"),
                sampleCard,
                sampleCard.copy(id = 5L, displayName = "Cashback", archived = false),
            ),
    )

@Preview(name = "Accounts categories", widthDp = 360)
@Preview(name = "Accounts categories large text", widthDp = 320, fontScale = 2f)
@Composable
private fun AccountsOverviewPreview() {
    PulseTheme(false) {
        HorizonTabScroll(PulseTokens.medium) {
            AccountsTabContent(sampleAccounts, enabled = true, onOpen = {})
        }
    }
}

@Preview(name = "Bank Accounts", widthDp = 360, heightDp = 800)
@Preview(name = "Bank Accounts large text", widthDp = 320, heightDp = 800, fontScale = 2f)
@Composable
private fun BankAccountsPreview() {
    PulseTheme(false) {
        AccountListScreen(AccountType.Bank, sampleAccounts, onAction = {})
    }
}

@Preview(name = "Credit Cards", widthDp = 360, heightDp = 800)
@Composable
private fun CreditCardsPreview() {
    PulseTheme(true) {
        AccountListScreen(AccountType.CreditCard, sampleAccounts, onAction = {})
    }
}

@Preview(name = "Edit Bank Account", widthDp = 360, heightDp = 800)
@Composable
private fun BankAccountEditorPreview() {
    PulseTheme(false) {
        AccountEditorScreen(AccountType.Bank, AccountsState(draft = sampleAccount), sampleAccount, onAction = {})
    }
}

@Preview(name = "Edit Credit Card", widthDp = 360, heightDp = 800)
@Composable
private fun CreditCardEditorPreview() {
    PulseTheme(true) {
        AccountEditorScreen(AccountType.CreditCard, AccountsState(draft = sampleCard), sampleCard, onAction = {})
    }
}

@Preview(name = "Finance phone light", widthDp = 360, heightDp = 800)
@Preview(name = "Finance tablet light", widthDp = 1024, heightDp = 900)
@Preview(name = "Finance large text", widthDp = 360, heightDp = 800, fontScale = 2f)
@Preview(name = "Finance landscape", widthDp = 800, heightDp = 360)
@Composable
private fun LightOverview() {
    FinancePreview(false)
}

@Preview(name = "Finance tablet dark", widthDp = 1024, heightDp = 900)
@Composable
private fun DarkOverview() {
    FinancePreview(true)
}

@Preview(name = "Projections phone", widthDp = 360)
@Preview(name = "Projections narrow", widthDp = 320)
@Preview(name = "Projections tablet", widthDp = 900)
@Preview(name = "Projections large text", widthDp = 360, fontScale = 2f)
@Composable
private fun ProjectionsPreview() {
    PulseTheme(false) {
        ProjectionCard(sample)
    }
}

@Preview(name = "Projections dark", widthDp = 360)
@Composable
private fun DarkProjectionsPreview() {
    PulseTheme(true) {
        ProjectionCard(sample)
    }
}

@Preview(name = "Projections empty", widthDp = 360)
@Composable
private fun EmptyProjectionsPreview() {
    PulseTheme(false) {
        ProjectionCard(sample.copy(projections = emptyList()))
    }
}

@Composable private fun FinancePreview(dark: Boolean) {
    PulseTheme(dark) {
        Surface {
            PulseAdaptiveLayout {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState(),
                        ).padding(PulseTokens.medium),
                    verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                ) {
                    PulseText("Preview fixtures", style = PulseTextStyle.Label)
                    HorizonMetrics(sample, true, {})
                    ProjectionCard(sample)
                }
            }
        }
    }
}

@Preview(name = "Finance editor dark", widthDp = 360, heightDp = 800)
@Preview(name = "Finance editor tablet", widthDp = 1024, heightDp = 900)
@Composable
private fun EditorPreview() {
    PulseTheme(true) {
        Surface {
            PulseFormLayout(
                scrollContentModifier = Modifier.padding(PulseTokens.medium),
                footer = { PreviewFormActions() },
            ) {
                TransactionEditor(
                    HorizonDraft(
                        HorizonForm.Transaction,
                        name = "Lunch",
                        amount = "12.50",
                        date = "2026-10-17",
                        currency = "USD",
                    ),
                    sample,
                    false,
                    null,
                    {},
                )
            }
        }
    }
}

@Preview(name = "Finance states", widthDp = 360, heightDp = 800)
@Composable
private fun StatesPreview() {
    PulseTheme {
        Surface {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                    ).padding(PulseTokens.medium),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                HorizonOverviewTabScreen(HorizonOverviewTabState(HorizonSection(), canMutate = false)) {}
                HorizonOverviewTabScreen(
                    HorizonOverviewTabState(HorizonSection(loading = false, error = Failure.Connection), canMutate = false),
                ) {}
                PulseEmptyState("No transactions", "Record your first transaction.")
                PulseButton("Saving transaction", {}, loading = true)
                PulseButton("Save transaction", {}, enabled = false)
            }
        }
    }
}

@Preview(name = "Fixed obligation phone", widthDp = 360, heightDp = 1000)
@Preview(name = "Fixed obligation tablet", widthDp = 1024, heightDp = 900)
@Preview(name = "Fixed obligation large text", widthDp = 360, heightDp = 1000, fontScale = 2f)
@Composable
private fun FixedObligationPreview() {
    ObligationPreviewContent(false)
}

@Preview(name = "Fixed obligation dark", widthDp = 360, heightDp = 1000)
@Composable
private fun FixedObligationDarkPreview() {
    ObligationPreviewContent(true)
}

@Composable
private fun ObligationPreviewContent(dark: Boolean) {
    PulseTheme(dark) {
        Surface(color = PulseTheme.colors.background) {
            PulseFormLayout(
                scrollContentModifier = Modifier.padding(PulseTokens.medium),
                footer = { PreviewFormActions() },
            ) {
                FixedObligationEditor(HorizonDraft(HorizonForm.Deduction), sample, false, null, {})
            }
        }
    }
}

@Composable
private fun PreviewFormActions() {
    PulseFormActions(
        primary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_save), {}),
        secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), {}),
        modifier = Modifier.padding(horizontal = PulseTokens.medium),
    )
}
