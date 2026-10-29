package dev.abhishekan.pulse.feature.horizon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun FixedObligationEditor(
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
    val keyboard = LocalSoftwareKeyboardController.current
    val editing = draft.id != null
    val saveLabel = stringResource(if (editing) R.string.pulse_horizon_obligation_save_changes else R.string.pulse_horizon_save_deduction)
    val cancelLabel = stringResource(R.string.pulse_horizon_cancel_editing)
    BoxWithConstraints(Modifier.widthIn(max = 1000.dp).fillMaxWidth().testTag("fixed-obligation-editor")) {
        val wide = maxWidth >= 600.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val categoryWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val categories = listOf("bill", "housing", "investment", "subscription", "debt", "other")
        val categoryScroll = rememberScrollState()
        val categoryStride = with(LocalDensity.current) { (categoryWidth + 12.dp).roundToPx() }
        LaunchedEffect(draft.id, wide) {
            if (!wide) categoryScroll.scrollTo(categories.indexOf(draft.category).coerceAtLeast(0) * categoryStride)
        }
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(color = Color.Transparent) {
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    ObligationFieldPair(wide, {
                        ObligationInput(
                            stringResource(R.string.pulse_horizon_name),
                            draft.name,
                            { onChange(draft.copy(name = it)) },
                            stringResource(R.string.pulse_horizon_obligation_name_hint),
                            !busy,
                            R.drawable.pulse_obligation_tag,
                        )
                    }, {
                        ObligationInput(
                            stringResource(R.string.pulse_horizon_amount),
                            draft.amount,
                            { onChange(draft.copy(amount = it)) },
                            "0.00",
                            !busy,
                            currency = true,
                            keyboardType = KeyboardType.Decimal,
                            accessibilityLabel = stringResource(R.string.pulse_horizon_amount_1_s, UserCurrency.symbol()),
                        )
                    })
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PulseText(stringResource(R.string.pulse_horizon_obligation_category), style = PulseTextStyle.Label)
                        if (wide) {
                            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                categories
                                    .chunked(3)
                                    .forEach { categories ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            categories.forEach { category ->
                                                ObligationCategory(
                                                    category,
                                                    draft.category == category,
                                                    !busy,
                                                    { onChange(draft.copy(category = category)) },
                                                    Modifier.weight(1f),
                                                    wide,
                                                )
                                            }
                                        }
                                    }
                            }
                        } else {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(categoryScroll).selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                categories.forEach { category ->
                                    ObligationCategory(
                                        category,
                                        draft.category == category,
                                        !busy,
                                        { onChange(draft.copy(category = category)) },
                                        Modifier.width(categoryWidth),
                                        false,
                                    )
                                }
                            }
                        }
                    }
                    ObligationFieldPair(wide, {
                        ObligationDayField(draft.dueDay, !busy) { onChange(draft.copy(dueDay = it)) }
                    }, {
                        ObligationBudget(draft, data, !busy, onChange)
                    })
                    PulseSwitch(
                        checked = draft.active,
                        onCheckedChange = { onChange(draft.copy(active = it)) },
                        label = stringResource(R.string.pulse_horizon_active_obligation),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        enabled = !busy,
                        supportingText = stringResource(R.string.pulse_horizon_obligation_active_hint),
                        switchAtEnd = true,
                    )
                    error?.let {
                        PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PulseButton(
                            stringResource(R.string.pulse_horizon_obligation_cancel),
                            onCancel,
                            Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = cancelLabel },
                            enabled = !busy,
                            variant = PulseButtonVariant.Secondary,
                        )
                        PulseButton(
                            if (editing || wide) saveLabel else stringResource(R.string.pulse_horizon_obligation_save),
                            {
                                keyboard?.hide()
                                onSave()
                            },
                            Modifier.weight(if (wide) 1.3f else 1f).heightIn(min = 56.dp).semantics { contentDescription = saveLabel },
                            enabled = !blocked && !busy && data != null,
                            loading = busy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObligationFieldPair(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            first()
            second()
        }
    }
}

@Composable
private fun ObligationInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    enabled: Boolean,
    icon: Int? = null,
    currency: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    accessibilityLabel: String = label,
) {
    val colors = PulseTheme.colors
    val keyboard = LocalSoftwareKeyboardController.current
    PulseTextField(
        value,
        onChange,
        label,
        Modifier.fillMaxWidth().semantics { contentDescription = accessibilityLabel },
        enabled = enabled,
        labelAbove = true,
        accessibilityLabel = accessibilityLabel,
        placeholder = { PulseText(hint, color = colors.textSecondary) },
        leadingIcon = {
            if (currency) PulseText(UserCurrency.symbol()) else icon?.let { Icon(painterResource(it), null, Modifier.size(20.dp)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
    )
}

@Composable
private fun ObligationCategory(
    category: String,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier,
    wide: Boolean,
) {
    val colors = PulseTheme.colors
    val accent =
        when (category) {
            "housing", "bill" -> colors.primary
            "investment" -> colors.sage
            "subscription" -> colors.coral
            "debt" -> colors.lavender
            else -> colors.textSecondary
        }
    val icon =
        when (category) {
            "housing" -> R.drawable.pulse_obligation_home
            "investment" -> R.drawable.pulse_obligation_bank
            "bill" -> R.drawable.pulse_obligation_document
            "subscription" -> R.drawable.pulse_obligation_recurring
            "debt" -> R.drawable.pulse_obligation_card
            else -> R.drawable.pulse_horizon_more
        }
    val tileColor by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.13f) else colors.background.copy(alpha = 0.45f),
        tween(160),
        label = "obligation-tile-color",
    )
    val borderColor by animateColorAsState(
        if (selected) colors.action else colors.border.copy(alpha = 0.65f),
        tween(160),
        label = "obligation-tile-border",
    )
    val iconScale by animateFloatAsState(if (selected) 1.04f else 1f, tween(160), label = "obligation-icon-scale")
    Surface(
        modifier
            .selectable(
                selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect,
            ).testTag("obligation-category-$category"),
        shape = PulseTheme.shapes.medium,
        color = tileColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        val badge: @Composable () -> Unit = {
            Box(Modifier.size(40.dp).background(accent.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(icon),
                    null,
                    Modifier.size(22.dp).graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                    tint = colors.textPrimary,
                )
            }
        }
        if (wide) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                badge()
                PulseText(horizonOption(category), style = PulseTextStyle.Body2)
            }
        } else {
            Column(
                Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                badge()
                PulseText(horizonOption(category), style = PulseTextStyle.Body2)
            }
        }
    }
}

@Composable
private fun ObligationBudget(
    draft: HorizonDraft,
    data: HorizonOverview?,
    enabled: Boolean,
    onChange: (HorizonDraft) -> Unit,
) {
    val budgets = data?.budgets.orEmpty()
    val none = stringResource(R.string.pulse_horizon_none)
    val selected =
        if (draft.budgetId == null) {
            none
        } else {
            budgets.find { it.id == draft.budgetId }?.name
                ?: stringResource(R.string.pulse_horizon_unavailable_selection)
        }
    PulseSelectionField(
        label = stringResource(R.string.pulse_horizon_obligation_budget_label),
        selected = selected,
        options = listOf(none) + budgets.map { it.name },
        onSelect = { index -> onChange(draft.copy(budgetId = budgets.getOrNull(index - 1)?.id)) },
        modifier = Modifier.fillMaxWidth(),
        triggerModifier = Modifier.heightIn(min = 56.dp).testTag("obligation-budget"),
        enabled = enabled,
    )
}
