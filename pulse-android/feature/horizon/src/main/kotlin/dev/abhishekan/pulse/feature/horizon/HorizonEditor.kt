package dev.abhishekan.pulse.feature.horizon

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
internal fun HorizonEditor(draft: HorizonDraft, data: HorizonOverview?, busy: Boolean, error: String?, blocked: Boolean,
    onChange: (HorizonDraft) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val enabled = !busy
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText("${if (draft.id == null) "Add" else "Edit"} ${draft.kind.label.lowercase()}", style = PulseTextStyle.Title)
        if (draft.kind == HorizonForm.Config) PulseText("Set the monthly starting pool and currency used by Financial Horizon.")
        if (draft.kind == HorizonForm.Purchase) PulseText("Planned for next month, as defined by the server.")
        if (draft.kind != HorizonForm.Config) PulseTextField(draft.name, { onChange(draft.copy(name = it)) }, "Name", Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
        if (draft.kind != HorizonForm.Category) PulseTextField(draft.amount, { onChange(draft.copy(amount = it)) },
            "Amount (${draft.currency})", Modifier.fillMaxWidth(), enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next))
        if (draft.kind == HorizonForm.Config) PulseTextField(draft.currency, { onChange(draft.copy(currency = it)) }, "Currency", Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Transaction) {
            Choice("Type", draft.type, listOf("debit", "credit"), { onChange(draft.copy(type = it)) }, enabled)
            PulseText("Debit is spending. Credit is income or a refund.")
        }
        if (draft.kind == HorizonForm.Transaction || draft.kind == HorizonForm.Subscription) {
            PulseTextButton("Date: ${draft.date.ifBlank { "Not set" }}", { datePicker = true }, enabled = enabled)
            if (draft.kind == HorizonForm.Transaction) {
                PulseTextButton("Time: ${draft.time}", { timePicker = true }, enabled = enabled)
                PulseText("Date and time use this device’s time zone; Pulse sends the matching instant.")
            } else if (draft.date.isNotBlank()) PulseTextButton("Clear renewal date", { onChange(draft.copy(date = "")) }, enabled = enabled)
        }
        if (draft.kind == HorizonForm.Deduction) {
            Choice("Obligation category", draft.category, listOf("housing", "investment", "bill", "subscription", "debt", "other"), { onChange(draft.copy(category = it)) }, enabled)
            PulseCheckbox(draft.active, { onChange(draft.copy(active = it)) }, "Active obligation", enabled = enabled)
        }
        if (draft.kind == HorizonForm.Subscription) {
            Choice("Billing cycle", draft.cycle, listOf("monthly", "yearly"), { onChange(draft.copy(cycle = it)) }, enabled)
            Choice("Status", draft.status, listOf("active", "paused", "cancelled"), { onChange(draft.copy(status = it)) }, enabled)
            PulseText("For yearly billing, the renewal date supplies the renewal month/day. The server calculates the next renewal.")
        }
        if (draft.kind in listOf(HorizonForm.Deduction, HorizonForm.Subscription)) PulseTextField(draft.dueDay,
            { onChange(draft.copy(dueDay = it)) }, "${if (draft.kind == HorizonForm.Subscription) "Billing" else "Due"} day (1–31, optional)",
            Modifier.fillMaxWidth(), enabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next))
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) {
            Relation("Category", draft.categoryId, data?.categories.orEmpty().map { it.id to it.name }, { onChange(draft.copy(categoryId = it)) }, enabled)
        }
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription, HorizonForm.Deduction)) {
            Relation("Budget", draft.budgetId, data?.budgets.orEmpty().map { it.id to it.name }, { onChange(draft.copy(budgetId = it)) }, enabled)
        }
        if (draft.kind == HorizonForm.Transaction) Relation("Subscription", draft.subscriptionId, data?.subscriptions.orEmpty().map { it.id to it.name }, { onChange(draft.copy(subscriptionId = it)) }, enabled)
        if (draft.kind == HorizonForm.Subscription) Relation("Fixed obligation", draft.deductionId, data?.deductions.orEmpty().map { it.id to it.name }, { onChange(draft.copy(deductionId = it)) }, enabled)
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) PulseTextField(draft.notes, { onChange(draft.copy(notes = it)) }, "Notes (optional)", Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Purchase) PulseTextField(draft.url, { onChange(draft.copy(url = it)) }, "URL (optional)", Modifier.fillMaxWidth(), enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }))
        if (draft.kind == HorizonForm.Category) {
            PulseTextField(draft.icon, { onChange(draft.copy(icon = it)) }, "Icon name", Modifier.fillMaxWidth(), enabled)
            PulseTextField(draft.color, { onChange(draft.copy(color = it)) }, "Color value", Modifier.fillMaxWidth(), enabled)
            PulseText("The category keeps this metadata for other Pulse clients. Android uses accessible theme colors.")
        }
        error?.let { PulseText(it, color = MaterialTheme.colorScheme.error) }
        PulseButton("Save ${draft.kind.label.lowercase()}", onSave, Modifier.fillMaxWidth(), enabled = !blocked && !busy && data != null, loading = busy)
        PulseTextButton("Cancel editing", onCancel, enabled = !busy)
    }
    if (datePicker) PulseDatePickerDialog(runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now()),
        { onChange(draft.copy(date = it.toString())); datePicker = false }, { datePicker = false })
    if (timePicker) PulseTimePickerDialog(runCatching { LocalTime.parse(draft.time) }.getOrDefault(LocalTime.MIDNIGHT),
        { onChange(draft.copy(time = it.toString())); timePicker = false }, { timePicker = false })
}

@Composable
internal fun Choice(label: String, value: String, options: List<String>, onSelect: (String) -> Unit, enabled: Boolean = true) {
    PulseSelectionField(label, value.replaceFirstChar(Char::uppercase), options.map { it.replaceFirstChar(Char::uppercase) }, { onSelect(options[it]) }, Modifier.fillMaxWidth(), enabled)
}
@Composable
private fun Relation(label: String, id: Long?, options: List<Pair<Long, String>>, onSelect: (Long?) -> Unit, enabled: Boolean) {
    val selected = if (id == null) "None" else options.find { it.first == id }?.second ?: "Unavailable selection"
    PulseSelectionField(label, selected, listOf("None") + options.map { it.second }, { onSelect(if (it == 0) null else options[it - 1].first) }, Modifier.fillMaxWidth(), enabled)
}
