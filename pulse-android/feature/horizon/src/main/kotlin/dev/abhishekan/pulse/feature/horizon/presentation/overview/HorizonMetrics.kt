package dev.abhishekan.pulse.feature.horizon.presentation.overview

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseBrandColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTone
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTileCard
import dev.abhishekan.pulse.feature.horizon.R
import java.math.RoundingMode

private val startingPoolTone = PulseTone(PulseBrandColors.indigo, Color(0xFF9B2BD8))

@Composable
internal fun HorizonMetrics(
    data: HorizonOverview,
    enabled: Boolean,
    configure: () -> Unit,
) {
    val colors = PulseTheme.colors
    val availablePoolTone = PulseTheme.featureTones.finance
    val warning = if (PulseTheme.isDark) colors.warning else Color(0xFFB26A00)
    val fixed = data.formatMoney(data.deductionsTotal)
    val subscriptions = data.formatMoney(data.subscriptionBurn)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fitsRow = maxWidth >= 840.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val cards: @Composable (Modifier) -> Unit = { modifier ->
            HorizonMetricCard(
                modifier = modifier.testTag("horizon-metric-starting"),
                title = stringResource(R.string.pulse_horizon_starting_pool_title),
                icon = R.drawable.pulse_horizon_wallet,
                value = data.formatMoney(data.baseAmount),
                description = stringResource(R.string.pulse_horizon_baseline_revenue),
                large = fitsRow,
                tone = startingPoolTone,
                iconBackground = Color.White.copy(alpha = 0.2f),
                descriptionColor = Color.White.copy(alpha = 0.85f),
                titleColor = Color.White.copy(alpha = 0.85f),
            ) {
                PulseButton(
                    stringResource(R.string.pulse_horizon_configure_starting_pool),
                    configure,
                    enabled = enabled,
                    style = PulseButtonStyle.Icon,
                ) {
                    Icon(
                        painterResource(R.drawable.pulse_horizon_edit),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                    )
                }
            }
            HorizonMetricCard(
                modifier = modifier.testTag("horizon-metric-committed"),
                title = stringResource(R.string.pulse_horizon_total_committed),
                icon = R.drawable.pulse_horizon_lock,
                value = data.formatMoney(data.deductionsTotal + data.subscriptionBurn),
                description = stringResource(R.string.pulse_horizon_committed_breakdown, fixed, subscriptions),
                large = fitsRow,
                iconBackground = colors.chip,
                descriptionColor = colors.textSecondary,
            ) {
                MetricBadge(
                    stringResource(
                        R.string.pulse_horizon_percent_of_pool,
                        data.committedPercent.setScale(1, RoundingMode.HALF_UP).toPlainString(),
                    ),
                    PulseBrandColors.gold.copy(alpha = 0.3f),
                    warning,
                )
            }
            HorizonMetricCard(
                modifier = modifier.testTag("horizon-metric-available"),
                title = stringResource(R.string.pulse_horizon_net_available_pool),
                icon = R.drawable.pulse_horizon_spark,
                value = data.formatMoney(data.remaining),
                description = stringResource(R.string.pulse_horizon_available_pool_description),
                large = fitsRow,
                tone = availablePoolTone,
                iconBackground = availablePoolTone.foreground.copy(alpha = 0.12f),
                descriptionColor = availablePoolTone.foreground.copy(alpha = 0.85f),
            ) {
                MetricBadge(
                    stringResource(if (data.isOvercommitted) R.string.pulse_horizon_deficit else R.string.pulse_horizon_surplus),
                    availablePoolTone.foreground.copy(alpha = 0.14f),
                    availablePoolTone.foreground,
                )
            }
        }
        if (fitsRow) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                cards(Modifier.weight(1f).fillMaxHeight().heightIn(min = 184.dp))
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                cards(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HorizonMetricCard(
    modifier: Modifier,
    title: String,
    @DrawableRes icon: Int,
    value: String,
    description: String,
    large: Boolean,
    iconBackground: Color,
    descriptionColor: Color,
    tone: PulseTone? = null,
    titleColor: Color = Color.Unspecified,
    action: @Composable () -> Unit,
) {
    val foreground = tone?.foreground ?: PulseTheme.colors.textPrimary
    val typography = PulseTheme.typography
    val content: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(40.dp).background(iconBackground, PulseTheme.shapes.control),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(icon), contentDescription = null, Modifier.size(20.dp), tint = foreground)
                }
                PulseText(
                    title,
                    Modifier.weight(1f),
                    style = typography.caption.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.05.em),
                    color = if (titleColor == Color.Unspecified) foreground else titleColor,
                )
                action()
            }
            PulseText(
                value,
                style = (if (large) typography.displayMedium else typography.heading2).copy(fontFeatureSettings = "tnum"),
                color = foreground,
            )
            PulseText(description, style = typography.body2, color = descriptionColor)
        }
    }
    if (tone == null) {
        PulseCard(modifier, contentPadding = PulseCardPadding.medium) { content() }
    } else {
        PulseTileCard(tone, modifier) { content() }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    background: Color,
    foreground: Color,
) {
    PulseText(
        label,
        Modifier
            .widthIn(max = 116.dp)
            .background(background, PulseTheme.shapes.pill)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        style = PulseTheme.typography.caption.copy(fontWeight = FontWeight.Bold, lineHeight = 15.sp),
        color = foreground,
    )
}
