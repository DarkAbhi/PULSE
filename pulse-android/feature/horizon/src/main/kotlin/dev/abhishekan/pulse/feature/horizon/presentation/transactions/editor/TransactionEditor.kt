package dev.abhishekan.pulse.feature.horizon.presentation.transactions.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonFormSection
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionFieldPair
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionInput
import dev.abhishekan.pulse.feature.horizon.presentation.components.HorizonCategoryPicker
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionRelation
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionSegments
import dev.abhishekan.pulse.feature.horizon.presentation.editor.TransactionPickerField
import java.time.LocalDate
import java.time.LocalTime

@Composable
internal fun TransactionEditor(
    draft: HorizonDraft,
    data: HorizonOverview?,
    busy: Boolean,
    error: UiText?,
    onChange: (HorizonDraft) -> Unit,
    accounts: List<FinancialAccount>? = null,
) {
    var picker by remember { mutableStateOf<String?>(null) }
    val focus = LocalFocusManager.current
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("transaction-editor")) {
        val wide = maxWidth >= 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = PulseTheme.colors.error) }
            HorizonFormSection(
                R.string.pulse_horizon_transaction_details,
                R.string.pulse_horizon_transaction_details_hint,
                R.drawable.pulse_obligation_card,
            ) {
                SubscriptionInput(draft.name, {
                    onChange(draft.copy(name = it))
                }, stringResource(R.string.pulse_horizon_name), stringResource(R.string.pulse_horizon_transaction_name_hint), !busy)
                SubscriptionInput(
                    draft.amount,
                    {
                        onChange(draft.copy(amount = it))
                    },
                    stringResource(
                        R.string.pulse_horizon_amount_1_s,
                        UserCurrency.symbol(),
                    ),
                    "0.00",
                    !busy,
                    KeyboardType.Decimal,
                    leadingIcon = {
                        PulseText(UserCurrency.symbol())
                    },
                )
                SubscriptionSegments(stringResource(R.string.pulse_horizon_type), draft.type, listOf("debit", "credit"), !busy) {
                    onChange(draft.copy(type = it))
                }
                PulseText(
                    stringResource(R.string.pulse_horizon_debit_credit_explanation),
                    style = PulseTextStyle.Body2,
                    color = PulseTheme.colors.textSecondary,
                )
            }
            HorizonFormSection(
                R.string.pulse_horizon_transaction_when,
                R.string.pulse_horizon_transaction_when_hint,
                R.drawable.pulse_obligation_calendar,
            ) {
                SubscriptionFieldPair(wide, {
                    TransactionPickerField(
                        R.string.pulse_horizon_transaction_date,
                        draft.date.ifBlank { stringResource(R.string.pulse_horizon_not_set) },
                        !busy,
                    ) {
                        focus.clearFocus()
                        picker =
                            "date"
                    }
                }, {
                    TransactionPickerField(R.string.pulse_horizon_transaction_time, draft.time, !busy) {
                        focus.clearFocus()
                        picker = "time"
                    }
                })
            }
            HorizonFormSection(
                R.string.pulse_horizon_transaction_organize,
                R.string.pulse_horizon_transaction_organize_hint,
                R.drawable.pulse_obligation_tag,
            ) {
                SubscriptionFieldPair(wide, {
                    HorizonCategoryPicker(
                        R.string.pulse_horizon_category,
                        draft.categoryId,
                        data?.categories.orEmpty(),
                        !busy,
                    ) { onChange(draft.copy(categoryId = it)) }
                }, {
                    SubscriptionRelation(
                        R.string.pulse_horizon_budget,
                        draft.budgetId,
                        data?.budgets.orEmpty().map {
                            it.id to it.name
                        },
                        !busy,
                    ) { onChange(draft.copy(budgetId = it)) }
                })
                SubscriptionRelation(
                    R.string.pulse_horizon_subscription,
                    draft.subscriptionId,
                    data?.subscriptions.orEmpty().map {
                        it.id to
                            it.name
                    },
                    !busy,
                ) { onChange(draft.copy(subscriptionId = it)) }
                if (draft.accountEditable) TransactionAccountField(draft, accounts, !busy, onChange)
            }
            HorizonFormSection(
                R.string.pulse_horizon_notes_optional,
                R.string.pulse_horizon_transaction_notes_hint,
                R.drawable.pulse_obligation_document,
            ) {
                PulseTextField(
                    value = draft.notes,
                    onValueChange = {
                        onChange(draft.copy(notes = it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6,
                    label =
                        stringResource(
                            R.string.pulse_horizon_notes_optional,
                        ),
                )
            }
        }
    }
    if (picker ==
        "date"
    ) {
        PulseDatePickerDialog(runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now(UserTimeZone.zone)), {
            onChange(draft.copy(date = it.toString()))
            picker =
                null
        }, { picker = null })
    }
    if (picker ==
        "time"
    ) {
        PulseTimePickerDialog(runCatching { LocalTime.parse(draft.time) }.getOrDefault(LocalTime.MIDNIGHT), {
            onChange(draft.copy(time = it.toString()))
            picker =
                null
        }, { picker = null })
    }
}

@Composable
private fun TransactionAccountField(
    draft: HorizonDraft,
    accounts: List<FinancialAccount>?,
    enabled: Boolean,
    onChange: (HorizonDraft) -> Unit,
) {
    val active = accounts.orEmpty().filterNot { it.archived }
    if (active.isEmpty() && draft.accountId == null) return
    val unassigned = stringResource(R.string.pulse_horizon_transaction_no_account)
    val archived = stringResource(R.string.pulse_horizon_account_archived)
    val selected = accounts?.firstOrNull { it.id == draft.accountId }
    val label =
        when {
            selected == null -> unassigned
            selected.archived -> "${selected.displayName} · $archived"
            else -> selected.displayName
        }
    PulseSelectionField(
        label = stringResource(R.string.pulse_horizon_transaction_account_optional),
        selected = label,
        options = listOf(unassigned) + active.map { it.displayName },
        onSelect = { onChange(draft.copy(accountId = active.getOrNull(it - 1)?.id)) },
        enabled = enabled && accounts != null,
    )
}
