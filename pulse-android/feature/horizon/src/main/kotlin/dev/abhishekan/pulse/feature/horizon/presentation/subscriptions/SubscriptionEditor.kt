package dev.abhishekan.pulse.feature.horizon.presentation.subscriptions

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
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
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonDraft
import dev.abhishekan.pulse.feature.horizon.presentation.editor.ObligationDayField
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionFieldPair
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionInput
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionRelation
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionSegments
import java.time.LocalDate

internal val subscriptionSections =
    listOf(
        R.string.pulse_horizon_subscription_basic,
        R.string.pulse_horizon_subscription_recurring,
        R.string.pulse_horizon_subscription_organization,
        R.string.pulse_horizon_subscription_notes,
    )

@Composable
internal fun SubscriptionEditor(
    draft: HorizonDraft,
    data: HorizonOverview?,
    busy: Boolean,
    error: UiText?,
    onChange: (HorizonDraft) -> Unit,
) {
    var datePicker by remember { mutableStateOf(false) }
    val colors = PulseTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val errorRequester = remember { BringIntoViewRequester() }
    LaunchedEffect(error) {
        if (error != null) errorRequester.bringIntoView()
    }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("subscription-editor")) {
        val wide = maxWidth >= 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            error?.let {
                PulseText(
                    it,
                    Modifier.bringIntoViewRequester(errorRequester).semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.error,
                )
            }
            SubscriptionSection(0, R.string.pulse_horizon_subscription_basic_hint, R.drawable.pulse_obligation_recurring) {
                SubscriptionInput(
                    draft.name,
                    { onChange(draft.copy(name = it)) },
                    stringResource(R.string.pulse_horizon_subscription_name_required),
                    stringResource(R.string.pulse_horizon_subscription_name_hint),
                    !busy,
                )
                SubscriptionFieldPair(wide, {
                    SubscriptionInput(
                        draft.amount,
                        { onChange(draft.copy(amount = it)) },
                        stringResource(R.string.pulse_horizon_subscription_amount, UserCurrency.symbol()),
                        "0.00",
                        !busy,
                        KeyboardType.Decimal,
                        leadingIcon = { PulseText(UserCurrency.symbol()) },
                    )
                }, {
                    SubscriptionSegments(
                        stringResource(R.string.pulse_horizon_billing_cycle),
                        draft.cycle,
                        listOf("monthly", "yearly"),
                        !busy,
                    ) { onChange(draft.copy(cycle = it)) }
                })
                SubscriptionSegments(
                    stringResource(R.string.pulse_horizon_status),
                    draft.status,
                    listOf("active", "paused", "cancelled"),
                    !busy,
                ) { onChange(draft.copy(status = it)) }
            }
            SubscriptionSection(1, R.string.pulse_horizon_subscription_recurring_hint, R.drawable.pulse_obligation_calendar) {
                if (draft.cycle == "monthly") {
                    ObligationDayField(
                        value = draft.dueDay,
                        enabled = !busy,
                        labelRes = R.string.pulse_horizon_billing_day_required,
                        titleRes = R.string.pulse_horizon_subscription_choose_billing_day,
                        helpRes = R.string.pulse_horizon_subscription_short_month,
                        clearRes = R.string.pulse_horizon_subscription_clear_billing_day,
                        tagPrefix = "subscription",
                    ) { onChange(draft.copy(dueDay = it)) }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseText(stringResource(R.string.pulse_horizon_subscription_renewal), style = PulseTextStyle.Label)
                        PulseButton(
                            draft.date.ifBlank { stringResource(R.string.pulse_horizon_not_set) },
                            {
                                keyboard?.hide()
                                datePicker = true
                            },
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("subscription-renewal-date"),
                            enabled = !busy,
                            variant = PulseButtonVariant.Secondary,
                            leadingIcon = { Icon(painterResource(R.drawable.pulse_obligation_calendar), null, Modifier.size(20.dp)) },
                        )
                    }
                }
                Surface(shape = PulseTheme.shapes.medium, color = colors.surfaceMuted) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PulseText(stringResource(R.string.pulse_horizon_subscription_how), style = PulseTextStyle.Label)
                        PulseText(
                            stringResource(
                                if (draft.cycle ==
                                    "monthly"
                                ) {
                                    R.string.pulse_horizon_subscription_monthly_help
                                } else {
                                    R.string.pulse_horizon_subscription_yearly_help
                                },
                            ),
                            style = PulseTextStyle.Body2,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            SubscriptionSection(2, R.string.pulse_horizon_subscription_organization_hint, R.drawable.pulse_obligation_tag) {
                SubscriptionFieldPair(wide, {
                    SubscriptionRelation(
                        R.string.pulse_horizon_category,
                        draft.categoryId,
                        data?.categories.orEmpty().map { it.id to it.name },
                        !busy,
                    ) { onChange(draft.copy(categoryId = it)) }
                }, {
                    SubscriptionRelation(
                        R.string.pulse_horizon_link_to_budget,
                        draft.budgetId,
                        data?.budgets.orEmpty().map { it.id to it.name },
                        !busy,
                    ) { onChange(draft.copy(budgetId = it)) }
                })
                SubscriptionRelation(
                    R.string.pulse_horizon_fixed_obligation,
                    draft.deductionId,
                    data?.deductions.orEmpty().map { it.id to it.name },
                    !busy,
                ) { onChange(draft.copy(deductionId = it)) }
            }
            SubscriptionSection(3, R.string.pulse_horizon_subscription_notes_hint, R.drawable.pulse_obligation_document) {
                PulseTextField(
                    value = draft.notes,
                    onValueChange = { onChange(draft.copy(notes = it)) },
                    modifier = Modifier.fillMaxWidth().testTag("subscription-notes"),
                    enabled = !busy,
                    singleLine = false,
                    minLines = 3,
                    maxLines = 6,
                    label = stringResource(R.string.pulse_horizon_notes_optional),
                    placeholder = {
                        PulseText(
                            stringResource(R.string.pulse_horizon_subscription_notes_placeholder),
                            color = colors.textSecondary,
                        )
                    },
                )
            }
        }
    }
    if (datePicker) {
        PulseDatePickerDialog(
            runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now(UserTimeZone.zone)),
            {
                onChange(draft.copy(date = it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
}

@Composable
private fun SubscriptionSection(
    index: Int,
    hint: Int,
    icon: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(
        Modifier.fillMaxWidth().testTag("subscription-section-$index"),
        shape = PulseTokens.cardShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = PulseTheme.shapes.medium, color = colors.primarySoft) {
                    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.onPrimarySoft)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(stringResource(subscriptionSections[index]), Modifier.semantics { heading() }, style = PulseTextStyle.Title3)
                    PulseText(stringResource(hint), style = PulseTextStyle.Body2, color = colors.textSecondary)
                }
            }
            content()
        }
    }
}
