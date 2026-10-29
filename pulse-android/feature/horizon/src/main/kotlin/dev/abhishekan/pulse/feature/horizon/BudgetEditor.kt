package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun BudgetEditor(
    draft: HorizonDraft,
    busy: Boolean,
    error: UiText?,
    showPreview: Boolean,
    canSave: Boolean,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
) {
    val colors = PulseTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current
    val nameFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val nameError = error != null && !validPurchaseName(draft.name)
    val amountError = error != null && !validPurchaseAmount(draft.amount)
    LaunchedEffect(error) {
        if (error != null) {
            when {
                nameError -> nameFocus.requestFocus()
                amountError -> amountFocus.requestFocus()
            }
        }
    }
    Surface(
        Modifier.fillMaxWidth().testTag("budget-editor"),
        shape = PulseTokens.cardShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.border),
    ) {
        BoxWithConstraints(Modifier.padding(24.dp)) {
            val sideBySide = maxWidth >= 640.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val details: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BudgetBadge(R.drawable.pulse_obligation_wallet)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PulseText(
                                stringResource(R.string.pulse_budget_details),
                                Modifier.semantics { heading() },
                                style = PulseTextStyle.Title3,
                            )
                            PulseText(
                                stringResource(R.string.pulse_budget_details_hint),
                                style = PulseTextStyle.Body2,
                                color = colors.textSecondary,
                            )
                        }
                    }
                    PulseTextField(
                        draft.name,
                        { onChange(draft.copy(name = it)) },
                        stringResource(R.string.pulse_budget_name),
                        Modifier.fillMaxWidth().focusRequester(nameFocus).testTag("budget-name"),
                        enabled = !busy,
                        labelAbove = true,
                        accessibilityLabel = stringResource(R.string.pulse_horizon_name),
                        error = if (nameError) uiText(R.string.pulse_budget_name_error) else null,
                        placeholder = { PulseText(stringResource(R.string.pulse_budget_name_hint), color = colors.textSecondary) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { amountFocus.requestFocus() }),
                    )
                    PulseTextField(
                        draft.amount,
                        { onChange(draft.copy(amount = it)) },
                        stringResource(R.string.pulse_budget_amount, UserCurrency.symbol()),
                        Modifier.fillMaxWidth().focusRequester(amountFocus).testTag("budget-amount"),
                        enabled = !busy,
                        labelAbove = true,
                        accessibilityLabel = stringResource(R.string.pulse_horizon_amount_1_s, UserCurrency.symbol()),
                        error = if (amountError) uiText(R.string.pulse_horizon_purchase_amount_error) else null,
                        textStyle = PulseTheme.typography.heading1,
                        leadingIcon = { PulseText(UserCurrency.symbol(), style = PulseTextStyle.Title3) },
                        placeholder = { PulseText("0.00", style = PulseTextStyle.Heading1, color = colors.textSecondary) },
                        supportingText = {
                            PulseText(
                                stringResource(R.string.pulse_budget_amount_hint),
                                style = PulseTextStyle.Body2,
                                color = colors.textSecondary,
                            )
                        },
                        visualTransformation = remember(UserCurrency.code) { PurchaseAmountTransformation(UserCurrency.code == "INR") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                if (canSave) {
                                    keyboard?.hide()
                                    onSave()
                                }
                            }),
                    )
                    if (error != null && !nameError && !amountError) {
                        PulseText(error, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error)
                    }
                }
            }
            if (showPreview && sideBySide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1.7f)) { details() }
                    Box(Modifier.weight(1f)) { BudgetPreview() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    details()
                    if (showPreview) BudgetPreview()
                }
            }
        }
    }
}

@Composable
private fun BudgetBadge(
    icon: Int,
    muted: Boolean = false,
) {
    val colors = PulseTheme.colors
    Surface(shape = CircleShape, color = if (muted) colors.surfaceMuted else colors.primarySoft) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = if (muted) colors.textPrimary else colors.onPrimarySoft)
        }
    }
}

@Composable
private fun BudgetPreview() {
    val colors = PulseTheme.colors
    Surface(
        Modifier.fillMaxWidth().testTag("budget-preview"),
        shape = PulseTheme.shapes.medium,
        color = colors.primarySoft.copy(alpha = 0.25f),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                BudgetBadge(R.drawable.pulse_budget_chart)
                PulseText(
                    stringResource(R.string.pulse_budget_preview),
                    Modifier.weight(1f).semantics { heading() },
                    style = PulseTextStyle.Title3,
                )
            }
            PulseText(stringResource(R.string.pulse_budget_preview_hint), style = PulseTextStyle.Body2, color = colors.textSecondary)
            PulseHorizontalDivider(color = colors.border)
            listOf(
                R.drawable.pulse_obligation_document to R.string.pulse_budget_preview_limit,
                R.drawable.pulse_budget_chart to R.string.pulse_budget_preview_compare,
                R.drawable.pulse_obligation_calendar to R.string.pulse_budget_preview_organize,
            ).forEach { (icon, text) ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BudgetBadge(icon, muted = true)
                    PulseText(stringResource(text), Modifier.weight(1f), style = PulseTextStyle.Body2, color = colors.textSecondary)
                }
            }
        }
    }
}
