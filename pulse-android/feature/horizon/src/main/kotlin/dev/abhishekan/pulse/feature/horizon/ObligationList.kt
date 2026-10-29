package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonDeduction
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.*
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.pulseDescription

@Composable
internal fun ObligationGrid(items: List<HorizonDeduction>, card: @Composable (HorizonDeduction) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("obligation-grid")) {
        val columns = if (maxWidth >= 560.dp && LocalDensity.current.fontScale <= 1.3f) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEach { item -> key(item.id) { Box(Modifier.weight(1f)) { card(item) } } }
                    repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun ObligationCard(item: HorizonDeduction, budgetName: String?, enabled: Boolean, edit: () -> Unit, delete: () -> Unit) {
    val colors = PulseTheme.colors
    val icon = when (item.category) {
        "housing" -> R.drawable.pulse_obligation_home
        "investment" -> R.drawable.pulse_budget_chart
        "bill" -> R.drawable.pulse_obligation_document
        "subscription" -> R.drawable.pulse_obligation_recurring
        "debt" -> R.drawable.pulse_obligation_card
        else -> R.drawable.pulse_obligation_wallet
    }
    Surface(Modifier.fillMaxWidth().testTag("horizon-record-${item.id}"), shape = PulseTokens.cardShape, color = colors.surface) {
        Column(Modifier.purchaseClouds().padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(56.dp).background(colors.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), null, Modifier.size(26.dp), tint = colors.textPrimary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(item.name, style = PulseTextStyle.Title3)
                    PulseText(currencyText(item.amount).asString(), style = PulseTextStyle.Metric)
                    PulseText(stringResource(
                        if (item.dueDay == null) R.string.pulse_horizon_obligation_status else R.string.pulse_horizon_obligation_status_with_due_day,
                        horizonOption(item.category), stringResource(if (item.active) R.string.pulse_horizon_active else R.string.pulse_horizon_inactive), item.dueDay ?: 0,
                    ), style = PulseTextStyle.Body2, color = colors.textSecondary)
                    if (budgetName != null) PulseText(stringResource(R.string.pulse_horizon_budget_1_s, budgetName), style = PulseTextStyle.Body2, color = colors.textSecondary)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseButton(stringResource(R.string.pulse_horizon_edit), edit, Modifier.pulseDescription(stringResource(R.string.pulse_horizon_edit_1_s, item.name)), enabled = enabled, leadingIcon = { Icon(painterResource(R.drawable.pulse_obligation_edit), null, Modifier.size(20.dp)) })
                PulseButton(stringResource(R.string.pulse_horizon_delete), delete, Modifier.pulseDescription(stringResource(R.string.pulse_horizon_delete_1_s, item.name)), enabled = enabled, variant = PulseButtonVariant.DestructiveSoft, leadingIcon = { Icon(painterResource(R.drawable.pulse_purchase_delete), null, Modifier.size(20.dp)) })
            }
        }
    }
}

