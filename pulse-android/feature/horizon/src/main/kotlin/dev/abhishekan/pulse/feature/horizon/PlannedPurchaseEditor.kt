package dev.abhishekan.pulse.feature.horizon

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseIconButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun PlannedPurchaseEditor(
    draft: HorizonDraft,
    data: HorizonOverview?,
    busy: Boolean,
    error: UiText?,
    blocked: Boolean,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = PulseTheme.colors
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val required = stringResource(R.string.pulse_horizon_purchase_required)
    val nameFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val urlFocus = remember { FocusRequester() }
    var submitted by rememberSaveable(draft.id) { mutableStateOf(false) }
    var urlVisited by rememberSaveable(draft.id) { mutableStateOf(false) }
    var urlFocused by remember { mutableStateOf(false) }
    val nameError = submitted && !validPurchaseName(draft.name)
    val amountError = submitted && !validPurchaseAmount(draft.amount)
    val urlError = (submitted || urlVisited) && !validPurchaseLink(draft.url)
    val save = {
        submitted = true
        when {
            !validPurchaseName(draft.name) -> nameFocus.requestFocus()
            !validPurchaseAmount(draft.amount) -> amountFocus.requestFocus()
            !validPurchaseLink(draft.url) -> urlFocus.requestFocus()
            else -> {
                focus.clearFocus()
                keyboard?.hide()
                onSave()
            }
        }
        Unit
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth().testTag("planned-purchase-editor"),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
        ) {
            Column(
                Modifier.animateContentSize(tween(PulseTokens.MOTION_MILLIS)),
                verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
            ) {
                PulseTextField(
                    draft.name,
                    { onChange(draft.copy(name = it)) },
                    stringResource(R.string.pulse_horizon_purchase_name),
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus)
                        .testTag("purchase-name")
                        .semantics { stateDescription = required },
                    enabled = !busy,
                    labelAbove = true,
                    error = if (nameError) uiText(R.string.pulse_horizon_purchase_name_error) else null,
                    placeholder = { PulseText(stringResource(R.string.pulse_horizon_purchase_name_hint), color = colors.textSecondary) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { amountFocus.requestFocus() }),
                )
                PulseTextField(
                    draft.amount,
                    { if (it.matches(Regex("[0-9]*(\\.[0-9]*)?"))) onChange(draft.copy(amount = it)) },
                    stringResource(R.string.pulse_horizon_purchase_amount),
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            amountFocus,
                        ).testTag("purchase-amount")
                        .semantics { stateDescription = required },
                    enabled = !busy,
                    labelAbove = true,
                    error = if (amountError) uiText(R.string.pulse_horizon_purchase_amount_error) else null,
                    textStyle = PulseTheme.typography.title1,
                    leadingIcon = {
                        PulseText(
                            if (UserCurrency.code ==
                                "INR"
                            ) {
                                "₹"
                            } else {
                                UserCurrency.symbol()
                            },
                            style = PulseTextStyle.Title3,
                        )
                    },
                    placeholder = {
                        PulseText(
                            stringResource(R.string.pulse_horizon_purchase_amount_placeholder),
                            style = PulseTextStyle.Title,
                            color = colors.textSecondary,
                        )
                    },
                    supportingText = {
                        PulseText(
                            stringResource(R.string.pulse_horizon_purchase_amount_hint),
                            style = PulseTextStyle.Body2,
                            color = colors.textSecondary,
                        )
                    },
                    visualTransformation = remember(UserCurrency.code) { PurchaseAmountTransformation(UserCurrency.code == "INR") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { urlFocus.requestFocus() }),
                )
                PulseTextField(
                    draft.url,
                    { onChange(draft.copy(url = it)) },
                    stringResource(R.string.pulse_horizon_purchase_link),
                    Modifier.fillMaxWidth().focusRequester(urlFocus).testTag("purchase-url").onFocusChanged {
                        if (urlFocused && !it.isFocused) urlVisited = true
                        urlFocused = it.isFocused
                    },
                    enabled = !busy,
                    labelAbove = true,
                    error = if (urlError) uiText(R.string.pulse_horizon_purchase_link_error) else null,
                    leadingIcon = { PurchaseIcon(if (urlError) R.drawable.pulse_purchase_error else R.drawable.pulse_purchase_link) },
                    trailingIcon = {
                        if (draft.url.isNotEmpty()) {
                            PulseIconButton(
                                stringResource(R.string.pulse_horizon_purchase_clear_link),
                                { onChange(draft.copy(url = "")) },
                                enabled = !busy,
                            ) {
                                PurchaseIcon(R.drawable.pulse_purchase_clear)
                            }
                        }
                    },
                    placeholder = { PulseText(stringResource(R.string.pulse_horizon_purchase_link_hint), color = colors.textSecondary) },
                    supportingText = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (draft.url.isNotBlank() && validPurchaseLink(draft.url)) PurchaseIcon(R.drawable.pulse_purchase_check)
                            PulseText(
                                stringResource(
                                    if (draft.url.isNotBlank() &&
                                        validPurchaseLink(draft.url)
                                    ) {
                                        R.string.pulse_horizon_purchase_link_valid
                                    } else {
                                        R.string.pulse_horizon_purchase_link_help
                                    },
                                ),
                                style = PulseTextStyle.Body2,
                                color = colors.textSecondary,
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (!busy && !blocked && data != null) save() }),
                )
            }
            error?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error) }
            if (data == null || blocked) {
                PulseText(
                    stringResource(
                        if (blocked) R.string.pulse_horizon_purchase_review_needed else R.string.pulse_horizon_purchase_loading_needed,
                    ),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
            }
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny),
            ) {
                PulseButton(
                    stringResource(
                        if (draft.id ==
                            null
                        ) {
                            R.string.pulse_horizon_purchase_add
                        } else {
                            R.string.pulse_horizon_obligation_save_changes
                        },
                    ),
                    save,
                    Modifier.fillMaxWidth().heightIn(min = PulseTokens.target),
                    enabled = !busy && !blocked && data != null,
                    loading = busy,
                )
                PulseTextButton(stringResource(R.string.pulse_horizon_purchase_cancel), onCancel, enabled = !busy)
            }
        }
    }
}

@Composable
private fun PurchaseIcon(resource: Int) {
    Icon(painterResource(resource), null, Modifier.size(PulseTokens.cardPadding), tint = PulseTheme.colors.textPrimary)
}
