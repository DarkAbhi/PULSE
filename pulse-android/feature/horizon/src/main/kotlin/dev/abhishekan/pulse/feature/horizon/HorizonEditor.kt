package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun HorizonEditor(
    draft: HorizonDraft,
    data: HorizonOverview?,
    busy: Boolean,
    error: UiText?,
    blocked: Boolean,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    if (draft.kind == HorizonForm.Deduction) {
        FixedObligationEditor(draft, data, busy, error, blocked, onChange, onSave, onCancel)
        return
    }
    var datePicker by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val enabled = !busy
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(
            stringResource(
                draft.kind.editorLabel(
                    if (draft.id ==
                        null
                    ) {
                        EditorAction.Add
                    } else {
                        EditorAction.Edit
                    },
                ),
            ),
            style = PulseTextStyle.Title,
        )
        if (draft.kind ==
            HorizonForm.Config
        ) {
            PulseText(
                stringResource(
                    R.string.pulse_horizon_set_the_monthly_starting_pool_and_currency_used_by,
                ),
            )
        }
        if (draft.kind ==
            HorizonForm.Purchase
        ) {
            PulseText(
                stringResource(
                    R.string.pulse_horizon_planned_for_next_month_as_defined_by_the_server,
                ),
            )
        }
        if (draft.kind !=
            HorizonForm.Config
        ) {
            PulseTextField(
                draft.name,
                {
                    onChange(draft.copy(name = it))
                },
                stringResource(R.string.pulse_horizon_name),
                Modifier.fillMaxWidth(),
                enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        if (draft.kind !=
            HorizonForm.Category
        ) {
            PulseTextField(
                draft.amount,
                { onChange(draft.copy(amount = it)) },
                stringResource(
                    R.string.pulse_horizon_amount_1_s,
                    dev.abhishekan.pulse.core.domain.model.UserCurrency
                        .symbol(),
                ),
                Modifier.fillMaxWidth(),
                enabled,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
            )
        }
        if (draft.kind == HorizonForm.Config) {
            PulseText(stringResource(R.string.pulse_horizon_currency_profile))
        }
        if (draft.kind == HorizonForm.Transaction) {
            Choice(
                stringResource(
                    R.string.pulse_horizon_type,
                ),
                draft.type,
                listOf("debit", "credit"),
                {
                    onChange(draft.copy(type = it))
                },
                enabled,
            )
            PulseText(stringResource(R.string.pulse_horizon_debit_credit_explanation))
        }
        if (draft.kind == HorizonForm.Transaction || draft.kind == HorizonForm.Subscription) {
            PulseTextButton(
                stringResource(
                    R.string.pulse_horizon_date_1_s,
                    draft.date.ifBlank {
                        stringResource(R.string.pulse_horizon_not_set)
                    },
                ),
                {
                    datePicker =
                        true
                },
                enabled = enabled,
            )
            if (draft.kind == HorizonForm.Transaction) {
                PulseTextButton(stringResource(R.string.pulse_horizon_time_1_s, draft.time), {
                    timePicker =
                        true
                }, enabled = enabled)
            } else if (draft.date.isNotBlank()) {
                PulseTextButton(stringResource(R.string.pulse_horizon_clear_renewal_date), {
                    onChange(draft.copy(date = ""))
                }, enabled = enabled)
            }
        }
        if (draft.kind == HorizonForm.Subscription) {
            Choice(
                stringResource(
                    R.string.pulse_horizon_billing_cycle,
                ),
                draft.cycle,
                listOf("monthly", "yearly"),
                {
                    onChange(draft.copy(cycle = it))
                },
                enabled,
            )
            Choice(
                stringResource(
                    R.string.pulse_horizon_status,
                ),
                draft.status,
                listOf("active", "paused", "cancelled"),
                {
                    onChange(draft.copy(status = it))
                },
                enabled,
            )
            PulseText(
                stringResource(
                    R.string.pulse_horizon_for_yearly_billing_the_renewal_date_supplies_the_renewal,
                ),
            )
        }
        if (draft.kind in
            listOf(HorizonForm.Deduction, HorizonForm.Subscription)
        ) {
            PulseTextField(
                draft.dueDay,
                {
                    onChange(draft.copy(dueDay = it))
                },
                stringResource(
                    R.string.pulse_horizon_1_s_day_1_31_optional,
                    if (draft.kind ==
                        HorizonForm.Subscription
                    ) {
                        stringResource(R.string.pulse_horizon_billing)
                    } else {
                        stringResource(R.string.pulse_horizon_due)
                    },
                ),
                Modifier.fillMaxWidth(),
                enabled,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
            )
        }
        if (draft.kind in listOf(HorizonForm.Transaction, HorizonForm.Subscription)) {
            Relation(
                stringResource(R.string.pulse_horizon_category),
                draft.categoryId,
                data?.categories.orEmpty().map {
                    it.id to
                        it.name
                },
                { onChange(draft.copy(categoryId = it)) },
                enabled,
            )
        }
        if (draft.kind in
            listOf(HorizonForm.Transaction, HorizonForm.Subscription, HorizonForm.Deduction)
        ) {
            Relation(
                stringResource(R.string.pulse_horizon_budget),
                draft.budgetId,
                data?.budgets.orEmpty().map {
                    it.id to
                        it.name
                },
                { onChange(draft.copy(budgetId = it)) },
                enabled,
            )
        }
        if (draft.kind ==
            HorizonForm.Transaction
        ) {
            Relation(
                stringResource(R.string.pulse_horizon_subscription),
                draft.subscriptionId,
                data?.subscriptions.orEmpty().map {
                    it.id to
                        it.name
                },
                { onChange(draft.copy(subscriptionId = it)) },
                enabled,
            )
        }
        if (draft.kind ==
            HorizonForm.Subscription
        ) {
            Relation(
                stringResource(R.string.pulse_horizon_fixed_obligation),
                draft.deductionId,
                data?.deductions.orEmpty().map {
                    it.id to
                        it.name
                },
                { onChange(draft.copy(deductionId = it)) },
                enabled,
            )
        }
        if (draft.kind in
            listOf(HorizonForm.Transaction, HorizonForm.Subscription)
        ) {
            PulseTextField(
                draft.notes,
                {
                    onChange(draft.copy(notes = it))
                },
                stringResource(R.string.pulse_horizon_notes_optional),
                Modifier.fillMaxWidth(),
                enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(onDone = {
                        keyboard?.hide()
                    }),
            )
        }
        if (draft.kind ==
            HorizonForm.Purchase
        ) {
            PulseTextField(
                draft.url,
                {
                    onChange(draft.copy(url = it))
                },
                stringResource(R.string.pulse_horizon_url_optional),
                Modifier.fillMaxWidth(),
                enabled,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(onDone = {
                        keyboard?.hide()
                    }),
            )
        }
        if (draft.kind == HorizonForm.Category) {
            PulseTextField(draft.icon, {
                onChange(draft.copy(icon = it))
            }, stringResource(R.string.pulse_horizon_icon_name), Modifier.fillMaxWidth(), enabled)
            PulseTextField(draft.color, {
                onChange(draft.copy(color = it))
            }, stringResource(R.string.pulse_horizon_color_value), Modifier.fillMaxWidth(), enabled)
            PulseText(
                stringResource(
                    R.string.pulse_horizon_the_category_keeps_this_metadata_for_other_pulse_clients,
                ),
            )
        }
        error?.let { PulseText(it, color = PulseTheme.colors.error) }
        PulseButton(
            stringResource(draft.kind.editorLabel(EditorAction.Save)),
            onSave,
            Modifier.fillMaxWidth(),
            enabled =
                !blocked && !busy && data != null,
            loading = busy,
        )
        PulseTextButton(
            stringResource(R.string.pulse_horizon_cancel_editing),
            onCancel,
            enabled = !busy,
        )
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching {
                LocalDate.parse(draft.date)
            }.getOrDefault(LocalDate.now(UserTimeZone.zone)),
            {
                onChange(draft.copy(date = it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
    if (timePicker) {
        PulseTimePickerDialog(
            runCatching {
                LocalTime.parse(draft.time)
            }.getOrDefault(LocalTime.MIDNIGHT),
            {
                onChange(draft.copy(time = it.toString()))
                timePicker = false
            },
            { timePicker = false },
        )
    }
}

@Composable
internal fun Choice(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    enabled: Boolean = true,
) {
    PulseSelectionField(
        label,
        horizonOption(value),
        options.map {
            horizonOption(it)
        },
        { onSelect(options[it]) },
        Modifier.fillMaxWidth(),
        enabled,
    )
}

@Composable
private fun Relation(
    label: String,
    id: Long?,
    options: List<Pair<Long, String>>,
    onSelect: (Long?) -> Unit,
    enabled: Boolean,
) {
    val selected =
        if (id ==
            null
        ) {
            stringResource(R.string.pulse_horizon_none)
        } else {
            options.find { it.first == id }?.second
                ?: stringResource(R.string.pulse_horizon_unavailable_selection)
        }
    PulseSelectionField(
        label,
        selected,
        listOf(stringResource(R.string.pulse_horizon_none)) + options.map { it.second },
        {
            onSelect(
                if (it ==
                    0
                ) {
                    null
                } else {
                    options[it - 1].first
                },
            )
        },
        Modifier.fillMaxWidth(),
        enabled,
    )
}
