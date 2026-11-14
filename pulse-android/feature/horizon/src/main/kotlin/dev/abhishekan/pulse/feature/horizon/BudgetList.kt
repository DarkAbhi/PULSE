package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonBudget
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseLinearProgressIndicator
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Composable
internal fun BudgetGrid(
    budgets: List<HorizonBudget>,
    card: @Composable (HorizonBudget) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("budget-grid")) {
        val columns =
            when {
                LocalDensity.current.fontScale > 1.3f -> 1
                maxWidth >= 840.dp -> 3
                maxWidth >= 560.dp -> 2
                else -> 1
            }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            budgets.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.forEach { budget ->
                        key(budget.id) { Box(Modifier.weight(1f)) { card(budget) } }
                    }
                    repeat(columns - group.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun BudgetCard(
    item: HorizonBudget,
    currency: String,
    enabled: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    val colors = PulseTheme.colors
    val appearance = budgetAppearance(item.name)
    val accent = if (item.isOverBudget) colors.error else appearance.second
    val background = lerp(colors.surface, accent, if (PulseTheme.isDark) 0.12f else 0.05f)
    Surface(
        Modifier.fillMaxWidth().testTag("horizon-record-${item.id}"),
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.dp, lerp(colors.surface, accent, 0.13f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(48.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(appearance.first), null, Modifier.size(26.dp), tint = accent)
                }
                Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PulseText(item.name, style = PulseTextStyle.Title3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PulseText(currencyText(item.used).asString(), style = PulseTextStyle.Label)
                        PulseText(
                            stringResource(R.string.pulse_horizon_budget_allocation, currencyText(item.allocated).asString()),
                            style = PulseTextStyle.Body2,
                        )
                    }
                }
                BudgetMenu(item, enabled, edit, delete)
            }
            PulseText(
                stringResource(
                    R.string.pulse_horizon_budget_percentage,
                    item.usagePercent.setScale(0, RoundingMode.HALF_UP).toPlainString(),
                ),
                Modifier.align(Alignment.End),
                style = PulseTextStyle.Label2,
                color = if (item.isOverBudget) colors.error else colors.textPrimary,
            )
            // The label preserves values above 100%; only the painted track is clamped.
            PulseLinearProgressIndicator(
                progress = {
                    item.usagePercent
                        .divide(BigDecimal(100))
                        .coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
                        .toFloat()
                },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = accent,
                trackColor = accent.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.pulse_obligation_wallet),
                    null,
                    Modifier.size(16.dp),
                    tint = colors.textSecondary,
                )
                PulseText(
                    stringResource(
                        if (item.isOverBudget) R.string.pulse_horizon_budget_over_by else R.string.pulse_horizon_budget_remaining,
                        currencyText(item.available.abs()).asString(),
                    ),
                    style = PulseTextStyle.Body2,
                    color = if (item.isOverBudget) colors.error else colors.textSecondary,
                )
            }
            if (currency.isBlank()) {
                PulseText(stringResource(R.string.pulse_horizon_currency_unavailable), style = PulseTextStyle.Body2)
            }
        }
    }
}

@Composable
private fun BudgetMenu(
    item: HorizonBudget,
    enabled: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
) {
    var expanded by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    Box {
        PulseButton(
            stringResource(R.string.pulse_horizon_budget_actions, item.name),
            { expanded = true },
            Modifier.testTag("budget-actions-${item.id}"),
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) {
            Icon(painterResource(R.drawable.pulse_horizon_more), null, tint = PulseTheme.colors.textSecondary)
        }
        PulseDropdownMenu(expanded && enabled, { expanded = false }) {
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_edit)) },
                modifier = Modifier.pulseDescription(stringResource(R.string.pulse_horizon_edit_1_s, item.name)),
                onClick = {
                    expanded = false
                    if (enabled) edit()
                },
            )
            PulseDropdownMenuItem(
                text = { PulseText(stringResource(R.string.pulse_horizon_delete), color = PulseTheme.colors.error) },
                modifier = Modifier.pulseDescription(stringResource(R.string.pulse_horizon_delete_1_s, item.name)),
                onClick = {
                    expanded = false
                    if (enabled) delete()
                },
            )
        }
    }
}

// Budgets have names but no category/icon field. Use familiar name cues and a stable fallback.
private fun budgetAppearance(name: String): Pair<Int, Color> {
    val normalized = name.lowercase(Locale.ROOT)
    return when {
        listOf("home", "house", "rent").any(normalized::contains) -> R.drawable.pulse_obligation_home to Color(0xFFEA713F)
        listOf("food", "dining", "grocery", "groceries").any(normalized::contains) -> R.drawable.pulse_budget_food to Color(0xFFF2AD28)
        listOf("transport", "travel", "fuel", "car").any(normalized::contains) -> R.drawable.pulse_budget_car to Color(0xFF36AC77)
        listOf("shop", "clothing").any(normalized::contains) -> R.drawable.pulse_purchase_bag to Color(0xFF6094F5)
        listOf("entertain", "game", "fun").any(normalized::contains) -> R.drawable.pulse_budget_game to Color(0xFF9768EC)
        else -> R.drawable.pulse_budget_misc to Color(0xFF48B88C)
    }
}
