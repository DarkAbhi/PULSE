package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R

internal enum class AccountType(
    val value: String,
    val label: Int,
    val listTitle: Int,
    val backToList: Int,
    val addTitle: Int,
    val editTitle: Int,
    val emptyLabel: Int,
    val icon: Int,
) {
    Bank(
        "bank_account",
        R.string.pulse_horizon_bank_account,
        R.string.pulse_horizon_bank_accounts,
        R.string.pulse_horizon_back_to_bank_accounts,
        R.string.pulse_horizon_add_bank_account,
        R.string.pulse_horizon_edit_bank_account,
        R.string.pulse_horizon_no_bank_accounts,
        R.drawable.pulse_obligation_bank,
    ),
    CreditCard(
        "credit_card",
        R.string.pulse_horizon_credit_card,
        R.string.pulse_horizon_credit_cards,
        R.string.pulse_horizon_back_to_credit_cards,
        R.string.pulse_horizon_add_credit_card,
        R.string.pulse_horizon_edit_credit_card,
        R.string.pulse_horizon_no_credit_cards,
        R.drawable.pulse_obligation_card,
    ),
}

internal val AccountType.softColor
    @Composable get() = if (this == AccountType.Bank) PulseTheme.colors.infoSoft else PulseTheme.colors.primarySoft

private val AccountType.accentColor
    @Composable get() = if (this == AccountType.Bank) PulseTheme.colors.info else PulseTheme.colors.onPrimarySoft

@Composable
internal fun AccountTypeBadge(type: AccountType) {
    Box(
        Modifier.size(PulseSpacing.large).background(PulseTheme.colors.surface, PulseTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(type.icon), contentDescription = null, modifier = Modifier.size(18.dp), tint = type.accentColor)
    }
}

@Composable
internal fun AccountInitialsBadge(
    type: AccountType,
    name: String,
    compact: Boolean = false,
) {
    val initials = name.asSequence().filter(Char::isLetterOrDigit).take(2).joinToString("").uppercase()
    Box(
        Modifier
            .size(if (compact) PulseSpacing.cardPadding else PulseSpacing.extraLarge)
            .background(
                color = if (compact) PulseTheme.colors.surface else type.softColor,
                shape = if (compact) PulseTheme.shapes.pill else PulseTheme.shapes.medium,
            ).clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isEmpty()) {
            Icon(painterResource(type.icon), contentDescription = null, modifier = Modifier.size(16.dp), tint = type.accentColor)
        } else {
            PulseText(
                initials,
                modifier = Modifier.padding(horizontal = PulseTokens.tiny),
                style = if (compact) PulseTextStyle.Caption else PulseTextStyle.Label,
                color = type.accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun FinancialAccountRow(
    type: AccountType,
    account: FinancialAccount,
    enabled: Boolean,
    onAction: (AccountsAction) -> Unit,
) {
    val colors = PulseTheme.colors
    val subtype =
        accountSubtypeLabels[account.bankAccountSubtype]?.takeIf {
            type == AccountType.Bank && account.bankAccountSubtype.isNotEmpty()
        }
    val hasBadges = subtype != null || account.archived
    val subtitle =
        listOfNotNull(
            account.institutionName,
            account.lastFour.takeIf { it.isNotBlank() }?.let { stringResource(R.string.pulse_horizon_account_masked_digits, it) },
            account.currency.takeIf { it.isNotBlank() },
        ).joinToString(" · ")

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val inlineBadges = maxWidth >= 268.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val badges: @Composable () -> Unit = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
            ) {
                subtype?.let { AccountTag(stringResource(it), type.softColor, type.accentColor) }
                if (account.archived) {
                    AccountTag(stringResource(R.string.pulse_horizon_account_archived), colors.surfaceMuted, colors.textSecondary)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = PulseTokens.small),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountInitialsBadge(type, account.displayName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                PulseText(account.displayName, style = PulseTextStyle.Label)
                PulseText(subtitle, style = PulseTextStyle.Body3, color = colors.textSecondary)
                if (!inlineBadges && hasBadges) badges()
            }
            if (inlineBadges && hasBadges) {
                Box(Modifier.widthIn(max = 96.dp)) { badges() }
            }
            AccountMenu(account, enabled, onAction)
        }
    }
}

@Composable
private fun AccountTag(
    label: String,
    background: Color,
    foreground: Color,
) {
    Surface(shape = PulseTheme.shapes.pill, color = background) {
        PulseText(
            label,
            modifier = Modifier.padding(horizontal = PulseTokens.small, vertical = PulseTokens.tiny),
            style = PulseTextStyle.Caption,
            color = foreground,
        )
    }
}

@Composable
private fun AccountMenu(
    account: FinancialAccount,
    enabled: Boolean,
    onAction: (AccountsAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_account_actions, account.displayName),
            { expanded = true },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) {
            Icon(painterResource(R.drawable.pulse_horizon_more), null, tint = PulseTheme.colors.textSecondary)
        }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_edit_account)) },
                onClick = {
                    expanded = false
                    onAction(AccountsAction.Edit(account))
                },
            )
            account.id?.let { id ->
                val label = if (account.archived) R.string.pulse_horizon_restore_account else R.string.pulse_horizon_archive_account
                PulseDropdownMenuItem(
                    text = { PulseText(stringResource(label)) },
                    onClick = {
                        expanded = false
                        onAction(AccountsAction.SetArchived(id, !account.archived))
                    },
                )
            }
        }
    }
}
