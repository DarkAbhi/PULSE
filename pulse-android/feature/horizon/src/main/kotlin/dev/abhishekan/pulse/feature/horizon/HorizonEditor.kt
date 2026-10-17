package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.horizon.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun HorizonEditor(draft: HorizonDraft, data: HorizonOverview?, busy: Boolean, error: UiText?, blocked: Boolean,
    onChange: (HorizonDraft) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val enabled = !busy
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(draft.kind.editorLabel(if (draft.id == null) EditorAction.Add else EditorAction.Edit), style = PulseTextStyle.Title)
        if (draft.kind == HorizonForm.Config) PulseText(uiText(R.string.pulse_horizon_set_the_monthly_starting_pool_and_currency_used_by))
        if (draft.kind == HorizonForm.Purchase) PulseText(uiText(R.string.pulse_horizon_planned_for_next_month_as_defined_by_the_server))
        if (draft.kind != HorizonForm.Config) PulseTextField(draft.name, { onChange(draft.copy(name = it)) }, uiText(R.string.pulse_horizon_name), Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        if (draft.kind != HorizonForm.Category) PulseTextField(draft.amount, { onChange(draft.copy(amount = it)) },
            uiText(R.string.pulse_horizon_amount_1_s , draft.currency), Modifier.fillMaxWidth(), enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
        if (draft.kind == HorizonForm.Config) PulseTextField(draft.currency, { onChange(draft.copy(currency = it)) }, uiText(R.string.pulse_horizon_currency), Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Transaction) {
            Choice(uiText(R.string.pulse_horizon_type), draft.type, listOf("debit", "credit"), { onChange(draft.copy(type = it)) }, enabled)
            PulseText(uiText(R.string.pulse_horizon_debit_credit_explanation))
        }
        if (draft.kind == HorizonForm.Transaction || draft.kind == HorizonForm.Subscription) {
            PulseTextButton(uiText(R.string.pulse_horizon_date_1_s , draft.date.ifBlank { uiText(R.string.pulse_horizon_not_set) }), { datePicker = true }, enabled = enabled)
            if (draft.kind == HorizonForm.Transaction) {
                PulseTextButton(uiText(R.string.pulse_horizon_time_1_s , draft.time), { timePicker = true }, enabled = enabled)
                PulseText(uiText(R.string.pulse_horizon_date_and_time_use_this_device_s_time_zone))
            } else if (draft.date.isNotBlank()) PulseTextButton(uiText(R.string.pulse_horizon_clear_renewal_date), { onChange(draft.copy(date = "")) }, enabled = enabled)
        }
        if (draft.kind == HorizonForm.Deduction) {
            Choice(uiText(R.string.pulse_horizon_obligation_category), draft.category, listOf("housing", "investment", "bill", "subscription", "debt", "other"), { onChange(draft.copy(category = it)) }, enabled)
            PulseCheckbox(draft.active, { onChange(draft.copy(active = it)) }, uiText(R.string.pulse_horizon_active_obligation), enabled = enabled)
        }
        if (draft.kind == HorizonForm.Subscription) {
            Choice(uiText(R.string.pulse_horizon_billing_cycle), draft.cycle, listOf("monthly", "yearly"), { onChange(draft.copy(cycle = it)) }, enabled)
            Choice(uiText(R.string.pulse_horizon_status), draft.status, listOf("active", "paused", "cancelled"), { onChange(draft.copy(status = it)) }, enabled)
            PulseText(uiText(R.string.pulse_horizon_for_yearly_billing_the_renewal_date_supplies_the_renewal))
        }
        if (draft.kind in listOf(HorizonForm.Deduction, HorizonForm.Subscription)) PulseTextField(draft.dueDay,
            { onChange(draft.copy(dueDay = it)) }, uiText(R.string.pulse_horizon_1_s_day_1_31_optional , if (draft.kind == HorizonForm.Subscription) uiText(R.string.pulse_horizon_billing) else uiText(R.string.pulse_horizon_due)),
            Modifier.fillMaxWidth(), enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) {
            Relation(uiText(R.string.pulse_horizon_category), draft.categoryId, data?.categories.orEmpty().map { it.id to it.name }, { onChange(draft.copy(categoryId = it)) }, enabled)
        }
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription, HorizonForm.Deduction)) {
            Relation(uiText(R.string.pulse_horizon_budget), draft.budgetId, data?.budgets.orEmpty().map { it.id to it.name }, { onChange(draft.copy(budgetId = it)) }, enabled)
        }
        if (draft.kind == HorizonForm.Transaction) Relation(uiText(R.string.pulse_horizon_subscription), draft.subscriptionId, data?.subscriptions.orEmpty().map { it.id to it.name }, { onChange(draft.copy(subscriptionId = it)) }, enabled)
        if (draft.kind == HorizonForm.Subscription) Relation(uiText(R.string.pulse_horizon_fixed_obligation), draft.deductionId, data?.deductions.orEmpty().map { it.id to it.name }, { onChange(draft.copy(deductionId = it)) }, enabled)
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) PulseTextField(draft.notes, { onChange(draft.copy(notes = it)) }, uiText(R.string.pulse_horizon_notes_optional), Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Purchase) PulseTextField(draft.url, { onChange(draft.copy(url = it)) }, uiText(R.string.pulse_horizon_url_optional), Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Category) {
            PulseTextField(draft.icon, { onChange(draft.copy(icon = it)) }, uiText(R.string.pulse_horizon_icon_name), Modifier.fillMaxWidth(), enabled)
            PulseTextField(draft.color, { onChange(draft.copy(color = it)) }, uiText(R.string.pulse_horizon_color_value), Modifier.fillMaxWidth(), enabled)
            PulseText(uiText(R.string.pulse_horizon_the_category_keeps_this_metadata_for_other_pulse_clients))
        }
        error?.let { PulseText(it, color = MaterialTheme.colorScheme.error) }
        PulseButton(draft.kind.editorLabel(EditorAction.Save), onSave, Modifier.fillMaxWidth(), enabled = !blocked && !busy && data != null, loading = busy)
        PulseTextButton(uiText(R.string.pulse_horizon_cancel_editing), onCancel, enabled = !busy)
    }
    if (datePicker) PulseDatePickerDialog(runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now()),
        { onChange(draft.copy(date = it.toString())); datePicker = false }, { datePicker = false })
    if (timePicker) PulseTimePickerDialog(runCatching { LocalTime.parse(draft.time) }.getOrDefault(LocalTime.MIDNIGHT),
        { onChange(draft.copy(time = it.toString())); timePicker = false }, { timePicker = false })
}

@Composable
internal fun Choice(label: UiText, value: String, options: List<String>, onSelect: (String) -> Unit, enabled: Boolean = true) {
    PulseSelectionField(label, horizonOption(value), options.map(::horizonOption), { onSelect(options[it]) }, Modifier.fillMaxWidth(), enabled)
}
@Composable
private fun Relation(label: UiText, id: Long?, options: List<Pair<Long, String>>, onSelect: (Long?) -> Unit, enabled: Boolean) {
    val selected = if (id == null) uiText(R.string.pulse_horizon_none) else options.find { it.first == id }?.second?.let(UiText::Literal) ?: uiText(R.string.pulse_horizon_unavailable_selection)
    PulseSelectionField(label, selected, listOf(uiText(R.string.pulse_horizon_none)) + options.map { UiText.Literal(it.second) }, { onSelect(if (it == 0) null else options[it - 1].first) }, Modifier.fillMaxWidth(), enabled)
}
