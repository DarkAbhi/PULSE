package dev.abhishekan.pulse.feature.horizon

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.domain.model.HorizonSubscription
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenu
import dev.abhishekan.pulse.core.ui.components.PulseDropdownMenuItem
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.currencyText
import dev.abhishekan.pulse.core.ui.pulseDescription
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@Composable
internal fun SubscriptionList(
    data: HorizonOverview,
    canMutate: Boolean,
    canViewPayments: Boolean,
    add: () -> Unit,
    edit: (HorizonSubscription) -> Unit,
    delete: (HorizonSubscription) -> Unit,
    payments: (HorizonSubscription) -> Unit,
) {
    val colors = PulseTheme.colors
    val today = LocalDate.now(UserTimeZone.zone)
    val active = data.subscriptions.filter { it.status == "active" }
    val upcoming = active.count { it.nextRenewal?.let { date -> ChronoUnit.DAYS.between(today, date) in 0L..7L } == true }
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("subscription-list")) {
        val columns =
            when {
                LocalDensity.current.fontScale > 1.3f -> 1
                maxWidth >= 1120.dp -> 3
                maxWidth >= 720.dp -> 2
                else -> 1
            }
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            val summaries =
                listOf(
                    Triple(
                        R.string.pulse_horizon_subscription_monthly_spend,
                        currencyText(data.subscriptionBurn).asString(),
                        R.drawable.pulse_obligation_card,
                    ),
                    Triple(R.string.pulse_horizon_subscription_active_count, active.size.toString(), R.drawable.pulse_obligation_recurring),
                    Triple(R.string.pulse_horizon_subscription_upcoming_count, upcoming.toString(), R.drawable.pulse_obligation_calendar),
                )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val summaryGroups =
                    when {
                        LocalDensity.current.fontScale > 1.3f -> summaries.chunked(1)
                        columns > 1 -> listOf(summaries)
                        else -> listOf(summaries.take(1), summaries.drop(1))
                    }
                summaryGroups.forEach { group ->
                    Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        group.forEach { (label, value, icon) ->
                            Surface(Modifier.weight(1f).fillMaxHeight(), shape = PulseTokens.cardShape, color = colors.surface) {
                                Row(
                                    Modifier.padding(20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (columns > 1 || group.size == 1) SubscriptionIconBadge(icon)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        PulseText(stringResource(label), style = PulseTextStyle.Body2, color = colors.textSecondary)
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            PulseText(value, style = PulseTextStyle.Heading2)
                                            if (label == R.string.pulse_horizon_subscription_upcoming_count) {
                                                PulseText(
                                                    stringResource(R.string.pulse_horizon_subscription_next_seven_days),
                                                    Modifier.align(Alignment.CenterVertically),
                                                    style = PulseTextStyle.Body2,
                                                    color = colors.warning,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            HorizonSectionAction(
                R.string.pulse_horizon_subscription_your_subscriptions,
                R.string.pulse_horizon_add_subscription,
                canMutate,
                add,
            )
            if (data.subscriptions.isEmpty()) {
                PulseEmptyState(
                    stringResource(R.string.pulse_horizon_no_subscriptions),
                    stringResource(R.string.pulse_horizon_add_a_monthly_or_yearly_subscription_to_track_its),
                )
            }
            data.subscriptions.chunked(columns).forEach { group ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    group.forEach { item ->
                        Box(Modifier.weight(1f)) {
                            SubscriptionListCard(
                                item,
                                data.categories
                                    .firstOrNull {
                                        it.id == item.categoryId
                                    }?.name,
                                today,
                                canMutate,
                                canViewPayments,
                                { edit(item) },
                                { delete(item) },
                                { payments(item) },
                            )
                        }
                    }
                    repeat(columns - group.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionListCard(
    item: HorizonSubscription,
    category: String?,
    today: LocalDate,
    canMutate: Boolean,
    canViewPayments: Boolean,
    edit: () -> Unit,
    delete: () -> Unit,
    payments: () -> Unit,
) {
    val colors = PulseTheme.colors
    val active = item.status == "active"
    Surface(
        Modifier.fillMaxWidth().testTag("horizon-record-${item.id}"),
        shape = PulseTokens.cardShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.divider),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(56.dp).background(colors.primarySoft, CircleShape), contentAlignment = Alignment.Center) {
                    PulseText(
                        item.name
                            .trim()
                            .take(1)
                            .uppercase(),
                        style = PulseTextStyle.Heading2,
                        color = colors.onPrimarySoft,
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PulseText(item.name, style = PulseTextStyle.Title3)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        category?.let { SubscriptionPill(it, colors.surfaceMuted, colors.textSecondary) }
                        SubscriptionPill(horizonOption(item.cycle), colors.surfaceMuted, colors.textSecondary)
                        SubscriptionPill(
                            horizonOption(item.status),
                            if (active) colors.successSoft else colors.surfaceMuted,
                            if (active) colors.success else colors.textSecondary,
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PulseText(currencyText(item.amount).asString(), style = PulseTextStyle.Heading2)
                PulseText(
                    stringResource(
                        if (item.cycle ==
                            "yearly"
                        ) {
                            R.string.pulse_horizon_subscription_per_year
                        } else {
                            R.string.pulse_horizon_subscription_per_month
                        },
                    ),
                    Modifier.align(Alignment.CenterVertically),
                    style = PulseTextStyle.Body2,
                    color = colors.textSecondary,
                )
            }
            PulseHorizontalDivider(color = colors.divider)
            val renewal = item.nextRenewal
            val days = renewal?.let { ChronoUnit.DAYS.between(today, it) }
            val urgent = active && days != null && days <= 3
            val soon = active && days != null && days in 4L..7L
            val renewalBackground =
                when {
                    urgent -> colors.errorSoft
                    soon -> colors.warningSoft
                    else -> colors.surfaceMuted.copy(alpha = 0.5f)
                }
            val renewalForeground =
                when {
                    urgent -> colors.error
                    soon -> colors.warning
                    else -> colors.textSecondary
                }
            Surface(shape = PulseTheme.shapes.medium, color = renewalBackground) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.pulse_obligation_calendar), null, Modifier.size(24.dp), tint = renewalForeground)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PulseText(
                            if (renewal != null &&
                                active
                            ) {
                                stringResource(
                                    R.string.pulse_horizon_subscription_renews_on,
                                    renewal.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                )
                            } else {
                                stringResource(
                                    if (active) {
                                        R.string.pulse_horizon_subscription_renewal_unknown
                                    } else {
                                        R.string.pulse_horizon_subscription_not_renewing
                                    },
                                )
                            },
                            style = PulseTextStyle.Body2,
                        )
                        if (active && days != null) {
                            PulseText(
                                when {
                                    days < 0 -> stringResource(R.string.pulse_horizon_subscription_renewal_past)
                                    days == 0L -> stringResource(R.string.pulse_horizon_subscription_renewal_today)
                                    else ->
                                        pluralStringResource(
                                            R.plurals.pulse_horizon_subscription_in_days,
                                            days.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                                            days,
                                        )
                                },
                                style = PulseTextStyle.Body2,
                                color = renewalForeground,
                            )
                        }
                        item.billingDay?.let {
                            PulseText(
                                stringResource(R.string.pulse_horizon_billing_day_1_s, it),
                                style = PulseTextStyle.Body2,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }
            // FlowRow keeps both metrics readable with large accessibility fonts.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SubscriptionDetail(
                    R.drawable.pulse_obligation_card,
                    pluralStringResource(R.plurals.pulse_horizon_subscription_payment_count, item.linkedCount, item.linkedCount),
                    stringResource(R.string.pulse_horizon_subscription_total_spent, currencyText(item.totalSpent).asString()),
                )
                SubscriptionDetail(
                    R.drawable.pulse_budget_chart,
                    currencyText(item.monthlyAmount).asString(),
                    stringResource(R.string.pulse_horizon_subscription_monthly_equivalent),
                )
            }
            item.notes?.takeIf { it.isNotBlank() }?.let { PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PulseButton(
                    stringResource(R.string.pulse_horizon_edit),
                    edit,
                    Modifier.pulseDescription(stringResource(R.string.pulse_horizon_edit_1_s, item.name)),
                    enabled = canMutate,
                    variant = PulseButtonVariant.Soft,
                )
                PulseButton(
                    stringResource(R.string.pulse_horizon_subscription_view_payments),
                    payments,
                    Modifier.pulseDescription(stringResource(R.string.pulse_horizon_view_payments_for_1_s, item.name)),
                    enabled = canViewPayments,
                    variant = PulseButtonVariant.Soft,
                )
                Box {
                    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
                    PulseButton(stringResource(R.string.pulse_horizon_subscription_more_actions, item.name), {
                        expanded = true
                    }, enabled = canMutate, style = PulseButtonStyle.Icon) {
                        Icon(painterResource(R.drawable.pulse_horizon_more), null, Modifier.size(24.dp), tint = colors.textSecondary)
                    }
                    PulseDropdownMenu(expanded && canMutate, { expanded = false }) {
                        PulseDropdownMenuItem(
                            text = { PulseText(stringResource(R.string.pulse_horizon_delete_1_s, item.name), color = colors.error) },
                            onClick = {
                                expanded = false
                                delete()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDetail(
    icon: Int,
    value: String,
    label: String,
) {
    val colors = PulseTheme.colors
    Surface(shape = PulseTheme.shapes.medium, color = colors.surfaceMuted.copy(alpha = 0.4f)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.textSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PulseText(value, style = PulseTextStyle.Label)
                PulseText(label, style = PulseTextStyle.Body2, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SubscriptionPill(
    text: String,
    background: Color,
    foreground: Color,
) {
    Surface(shape = CircleShape, color = background) {
        PulseText(text, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = PulseTextStyle.Body2, color = foreground)
    }
}

@Composable
private fun SubscriptionIconBadge(icon: Int) {
    val colors = PulseTheme.colors
    Box(
        Modifier.size(48.dp).background(colors.primarySoft.copy(alpha = 0.6f), PulseTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, Modifier.size(24.dp), tint = colors.action)
    }
}
