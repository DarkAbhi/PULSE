package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun TransactionEditor(draft: HorizonDraft, data: HorizonOverview?, busy: Boolean, error: UiText?, onChange: (HorizonDraft) -> Unit) {
    var picker by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("transaction-editor")) {
        val wide = maxWidth >= 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
            TransactionSection(R.string.transaction_details, R.string.transaction_details_hint, R.drawable.pulse_obligation_card) {
                SubscriptionInput(draft.name, { onChange(draft.copy(name = it)) }, stringResource(R.string.pulse_horizon_name), stringResource(R.string.transaction_name_hint), !busy)
                SubscriptionInput(draft.amount, { onChange(draft.copy(amount = it)) }, stringResource(R.string.pulse_horizon_amount_1_s, UserCurrency.symbol()), "0.00", !busy, KeyboardType.Decimal, leadingIcon = { PulseText(UserCurrency.symbol()) })
                SubscriptionSegments(stringResource(R.string.pulse_horizon_type), draft.type, listOf("debit", "credit"), !busy) { onChange(draft.copy(type = it)) }
                PulseText(stringResource(R.string.pulse_horizon_debit_credit_explanation), style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
            }
            TransactionSection(R.string.transaction_when, R.string.transaction_when_hint, R.drawable.pulse_obligation_calendar) {
                SubscriptionFieldPair(wide, {
                    TransactionPickerField(R.string.transaction_date, draft.date.ifBlank { stringResource(R.string.pulse_horizon_not_set) }, !busy) { focus.clearFocus(); picker = "date" }
                }, {
                    TransactionPickerField(R.string.transaction_time, draft.time, !busy) { focus.clearFocus(); picker = "time" }
                })
            }
            TransactionSection(R.string.transaction_organize, R.string.transaction_organize_hint, R.drawable.pulse_obligation_tag) {
                SubscriptionFieldPair(wide, {
                    SubscriptionRelation(R.string.pulse_horizon_category, draft.categoryId, data?.categories.orEmpty().map { it.id to it.name }, !busy) { onChange(draft.copy(categoryId = it)) }
                }, {
                    SubscriptionRelation(R.string.pulse_horizon_budget, draft.budgetId, data?.budgets.orEmpty().map { it.id to it.name }, !busy) { onChange(draft.copy(budgetId = it)) }
                })
                SubscriptionRelation(R.string.pulse_horizon_subscription, draft.subscriptionId, data?.subscriptions.orEmpty().map { it.id to it.name }, !busy) { onChange(draft.copy(subscriptionId = it)) }
            }
            TransactionSection(R.string.pulse_horizon_notes_optional, R.string.transaction_notes_hint, R.drawable.pulse_obligation_document) {
                PulseTextField(value = draft.notes, onValueChange = { onChange(draft.copy(notes = it)) }, modifier = Modifier.fillMaxWidth(), enabled = !busy, singleLine = false, minLines = 3, maxLines = 6, label = stringResource(R.string.pulse_horizon_notes_optional))
            }
        }
    }
    if (picker == "date") PulseDatePickerDialog(runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now(UserTimeZone.zone)), { onChange(draft.copy(date = it.toString())); picker = null }, { picker = null })
    if (picker == "time") PulseTimePickerDialog(runCatching { LocalTime.parse(draft.time) }.getOrDefault(LocalTime.MIDNIGHT), { onChange(draft.copy(time = it.toString())); picker = null }, { picker = null })
}

@Composable
private fun TransactionPickerField(label: Int, value: String, enabled: Boolean, onClick: () -> Unit) {
    val title = stringResource(label)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PulseText(title, style = PulseTextStyle.Label)
        PulseButton(value, onClick, Modifier.fillMaxWidth().heightIn(min = 56.dp).semantics { contentDescription = "$title: $value" }, enabled = enabled, variant = PulseButtonVariant.Secondary)
    }
}

@Composable
private fun TransactionSection(title: Int, hint: Int, icon: Int, content: @Composable ColumnScope.() -> Unit) {
    val colors = PulseTheme.colors
    Surface(Modifier.fillMaxWidth(), shape = PulseTokens.cardShape, color = colors.surface, border = BorderStroke(1.dp, colors.border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = PulseTheme.shapes.medium, color = colors.primarySoft) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimarySoft) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(stringResource(title), Modifier.semantics { heading() }, style = PulseTextStyle.Title3)
                    PulseText(stringResource(hint), style = PulseTextStyle.Body2, color = colors.textSecondary)
                }
            }
            content()
        }
    }
}
