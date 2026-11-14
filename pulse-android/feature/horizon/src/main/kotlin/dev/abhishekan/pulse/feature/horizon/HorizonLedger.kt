package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonTransaction
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal fun ledgerMonths(items: List<HorizonTransaction>) =
    items
        .sortedWith(
            compareByDescending<HorizonTransaction> {
                it.date
            }.thenByDescending { it.id },
        ).groupBy { YearMonth.from(it.date.atZone(UserTimeZone.zone)) }

@Composable
internal fun TransactionDetails(
    transaction: HorizonTransaction,
    currency: String,
    enabled: Boolean,
    dismiss: () -> Unit,
    edit: (HorizonTransaction) -> Unit,
    delete: (HorizonTransaction) -> Unit,
) {
    PulseBottomSheet(title = transaction.name, onDismissRequest = dismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 24.dp),
        ) {
            TransactionCard(transaction, currency, false)
            Actions(
                transaction.name,
                enabled,
                {
                    dismiss()
                    edit(transaction)
                },
                {
                    dismiss()
                    delete(transaction)
                },
            )
        }
    }
}

@Composable
internal fun LedgerRow(
    tx: HorizonTransaction,
    wide: Boolean,
    open: (() -> Unit)? = null,
) {
    val credit = tx.type == "credit"
    val accent = if (credit) PulseTheme.colors.success else PulseTheme.colors.action
    Row(
        Modifier
            .fillMaxWidth()
            .testTag("horizon-record-${tx.id}")
            .then(if (open != null) Modifier.clickable(onClick = open) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.12f), PulseTheme.shapes.medium), contentAlignment = Alignment.Center) {
            PulseText(tx.name.take(1).uppercase(), style = PulseTextStyle.Title, color = accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PulseText(tx.name, style = PulseTextStyle.Title3)
            PulseText(
                DateTimeFormatter.ofPattern("d MMM · h:mm a").withZone(UserTimeZone.zone).format(tx.date),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.textSecondary,
            )
            if (!wide) LedgerBadges(tx)
        }
        if (wide) Box(Modifier.weight(0.8f)) { LedgerBadges(tx) }
        PulseText(
            stringResource(R.string.pulse_horizon_signed_amount, if (credit) "+" else "−", currencyText(tx.amount).asString()),
            style = PulseTheme.typography.title3.copy(fontFeatureSettings = "tnum"),
            color = if (credit) PulseTheme.colors.success else PulseTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun LedgerBadges(tx: HorizonTransaction) {
    // Wrap long category names rather than forcing the amount off a narrow screen.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PulseText(
            tx.categoryName,
            Modifier.background(PulseTheme.colors.primarySoft, PulseTheme.shapes.small).padding(horizontal = 8.dp, vertical = 4.dp),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.onPrimarySoft,
        )
        PulseText(
            stringResource(if (tx.type == "credit") R.string.pulse_horizon_credit else R.string.pulse_horizon_debit),
            Modifier
                .background(
                    if (tx.type ==
                        "credit"
                    ) {
                        PulseTheme.colors.successSoft
                    } else {
                        PulseTheme.colors.surfaceMuted
                    },
                    PulseTheme.shapes.small,
                ).padding(horizontal = 8.dp, vertical = 4.dp),
            style = PulseTextStyle.Caption,
            color = if (tx.type == "credit") PulseTheme.colors.success else PulseTheme.colors.textSecondary,
        )
    }
}
