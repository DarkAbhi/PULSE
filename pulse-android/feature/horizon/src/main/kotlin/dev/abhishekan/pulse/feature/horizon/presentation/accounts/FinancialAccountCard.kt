package dev.abhishekan.pulse.feature.horizon.presentation.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseSpacing
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
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
    val emptyMessage: Int,
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
        R.string.pulse_horizon_no_bank_accounts_help,
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
        R.string.pulse_horizon_no_credit_cards_help,
        R.drawable.pulse_obligation_card,
    ),
}

internal val AccountType.softColor
    @Composable get() = if (this == AccountType.Bank) PulseTheme.colors.infoSoft else PulseTheme.colors.primarySoft

internal val AccountType.accentColor
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
    val initials =
        name
            .asSequence()
            .filter(Char::isLetterOrDigit)
            .take(2)
            .joinToString("")
            .uppercase()
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
