package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.PlannedPurchase
import dev.abhishekan.pulse.core.domain.model.PlannedPurchases
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText

@Composable
internal fun PlannedPurchaseList(
    purchases: PlannedPurchases,
    currency: String,
    enabled: Boolean,
    add: () -> Unit,
    delete: (PlannedPurchase) -> Unit,
    clear: () -> Unit,
) {
    val colors = PulseTheme.colors
    val empty = purchases.items.isEmpty()
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("purchase-summary"),
        shape = PulseTokens.cardShape,
        color = colors.surface,
    ) {
        BoxWithConstraints(Modifier.purchaseClouds().padding(24.dp)) {
            val wide = maxWidth >= 640.dp && LocalDensity.current.fontScale <= 1.3f
            val summary: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (wide) PurchaseBadge(summary = true)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!wide) PurchaseIcon(R.drawable.pulse_purchase_cart)
                            PulseText(
                                stringResource(R.string.pulse_horizon_purchase_summary_eyebrow),
                                style = PulseTextStyle.Overline,
                                color = colors.textPrimary,
                            )
                        }
                        PurchaseAmount(
                            stringResource(
                                R.string.pulse_horizon_purchase_summary_total,
                                currencyText(purchases.total).asString(),
                            ),
                            Modifier.semantics {
                                heading()
                            },
                        )
                        PulseText(
                            pluralStringResource(
                                R.plurals.pulse_horizon_purchase_summary_count,
                                purchases.items.size,
                                purchases.items.size,
                            ),
                            style = PulseTextStyle.Body2,
                            color = colors.textSecondary,
                        )
                        if (currency.isBlank()) {
                            PulseText(
                                stringResource(R.string.pulse_horizon_currency_unavailable),
                                style = PulseTextStyle.Body2,
                            )
                        }
                    }
                }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f)) { summary() }
                    if (!empty) AddPurchaseButton(add, enabled)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    summary()
                    if (!empty) AddPurchaseButton(add, enabled, Modifier.fillMaxWidth())
                }
            }
        }
    }
    if (empty) {
        Surface(shape = PulseTokens.cardShape, color = colors.surface, modifier = Modifier.fillMaxWidth().testTag("purchase-empty")) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    PurchaseBadge(summary = true)
                }
                PurchaseTitle(stringResource(R.string.pulse_horizon_nothing_planned_yet), Modifier.semantics { heading() })
                PulseText(
                    stringResource(R.string.pulse_horizon_purchase_empty_body),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
                PulseButton(
                    stringResource(R.string.pulse_horizon_purchase_first),
                    add,
                    Modifier.fillMaxWidth(),
                    enabled = enabled,
                    leadingIcon = { PurchaseIcon(R.drawable.pulse_purchase_add) },
                )
            }
        }
    } else {
        PurchaseTitle(
            pluralStringResource(R.plurals.pulse_horizon_purchase_list_count, purchases.items.size, purchases.items.size),
            Modifier.semantics { heading() },
        )
        purchases.items.forEach { item -> PurchaseListCard(item, enabled) { delete(item) } }
        PulseHorizontalDivider(color = colors.divider)
        PulseButton(
            stringResource(R.string.pulse_horizon_clear_planned_purchases),
            clear,
            enabled = enabled,
            variant = PulseButtonVariant.Destructive,
            leadingIcon = { PurchaseIcon(R.drawable.pulse_purchase_delete) },
            style = PulseButtonStyle.Text,
        )
    }
}

@Composable
private fun PurchaseListCard(
    item: PlannedPurchase,
    enabled: Boolean,
    delete: () -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("horizon-record-${item.id}"),
        shape = PulseTokens.cardShape,
        color = colors.surface,
    ) {
        BoxWithConstraints(Modifier.padding(24.dp)) {
            val details: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PurchaseTitle(item.name)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PurchaseIcon(R.drawable.pulse_obligation_calendar, secondary = true)
                        PulseText(
                            stringResource(R.string.pulse_horizon_purchase_planned_next_month),
                            style = PulseTextStyle.Body2,
                            color = colors.textSecondary,
                        )
                    }
                    item.url?.takeIf { it.isNotBlank() }?.let { url ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                            PurchaseIcon(R.drawable.pulse_purchase_link, secondary = true)
                            PulseText(url, Modifier.weight(1f), style = PulseTextStyle.Body2, color = colors.textSecondary)
                        }
                    }
                }
            }
            if (maxWidth >= 760.dp && LocalDensity.current.fontScale <= 1.3f) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    PurchaseBadge()
                    Box(Modifier.weight(1f)) { details() }
                    // Separate the value and action without forcing a height on wrapping content.
                    Box(Modifier.background(colors.divider).size(1.dp, 72.dp))
                    PurchaseAmount(currencyText(item.price).asString(), Modifier.weight(0.6f))
                    Box(Modifier.background(colors.divider).size(1.dp, 72.dp))
                    DeletePurchaseButton(item, enabled, delete)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        PurchaseBadge()
                        Box(Modifier.weight(1f)) { details() }
                    }
                    PulseHorizontalDivider(color = colors.divider)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PurchaseAmount(currencyText(item.price).asString(), Modifier.weight(1f))
                        DeletePurchaseButton(item, enabled, delete)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletePurchaseButton(
    item: PlannedPurchase,
    enabled: Boolean,
    delete: () -> Unit,
) {
    val colors = PulseTheme.colors
    Surface(
        shape = PulseTheme.shapes.medium,
        color = colors.error.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, colors.error.copy(alpha = if (enabled) 0.24f else 0.10f)),
    ) {
        PulseButton(
            stringResource(R.string.pulse_horizon_delete_1_s, item.name),
            delete,
            enabled = enabled,
            style = PulseButtonStyle.Icon,
        ) {
            Icon(
                painterResource(R.drawable.pulse_purchase_delete),
                null,
                Modifier.size(24.dp),
                tint = if (enabled) colors.error else colors.textSecondary,
            )
        }
    }
}

@Composable
private fun AddPurchaseButton(
    add: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    PulseButton(
        stringResource(R.string.pulse_horizon_purchase_add),
        add,
        modifier,
        enabled = enabled,
        leadingIcon = { PurchaseIcon(R.drawable.pulse_purchase_add) },
    )
}

@Composable
private fun PurchaseAmount(
    text: String,
    modifier: Modifier = Modifier,
) {
    PulseText(
        text,
        modifier,
        style = PulseTextStyle.Heading2,
        color = PulseTheme.colors.textPrimary,
    )
}

@Composable
private fun PurchaseTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    PulseText(
        text,
        modifier,
        style = PulseTextStyle.Title3,
        color = PulseTheme.colors.textPrimary,
    )
}

@Composable
private fun PurchaseIcon(
    resource: Int,
    secondary: Boolean = false,
) {
    Icon(
        painterResource(resource),
        null,
        Modifier.size(20.dp),
        tint = if (secondary) PulseTheme.colors.textSecondary else androidx.compose.material3.LocalContentColor.current,
    )
}

@Composable
private fun PurchaseBadge(summary: Boolean = false) {
    val colors = PulseTheme.colors
    Box(
        Modifier
            .size(
                if (summary) 72.dp else 64.dp,
            ).background(
                if (summary) colors.primary.copy(alpha = 0.14f) else colors.primarySoft.copy(alpha = 0.6f),
                if (summary) CircleShape else PulseTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(44.dp).then(if (summary) Modifier.background(colors.primary.copy(alpha = 0.16f), CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.pulse_purchase_cart), null, Modifier.size(32.dp), tint = colors.onPrimarySoft)
        }
    }
}

/** Broad bezier lobes echo the reference's layered clouds at any aspect ratio. */
@Composable
private fun Modifier.purchaseClouds(): Modifier {
    val colors = PulseTheme.colors
    return drawBehind {
        val w = size.width
        val h = size.height
        val lowerCloud =
            Path().apply {
                moveTo(w * 0.56f, h)
                cubicTo(w * 0.61f, h * 0.70f, w * 0.69f, h * 0.58f, w * 0.77f, h * 0.59f)
                cubicTo(w * 0.85f, h * 0.60f, w * 0.87f, h * 0.34f, w, h * 0.28f)
                lineTo(w, h)
                close()
            }
        val upperCloud =
            Path().apply {
                moveTo(w * 0.65f, h)
                cubicTo(w * 0.72f, h, w * 0.72f, h * 0.50f, w * 0.77f, h * 0.27f)
                cubicTo(w * 0.82f, h * 0.02f, w * 0.87f, 0f, w * 0.90f, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                close()
            }
        drawPath(lowerCloud, colors.primary.copy(alpha = 0.07f))
        drawPath(upperCloud, Brush.horizontalGradient(listOf(colors.primary.copy(alpha = 0.04f), colors.primary.copy(alpha = 0.13f))))
    }
}
