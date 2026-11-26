package dev.abhishekan.pulse.feature.horizon.presentation.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.PulseBrandColors
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCardPadding
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.horizon.R
import java.math.BigDecimal

private val projectionMonths = listOf(3, 6, 12)

@Composable
internal fun ProjectionCard(data: HorizonOverview) {
    val colors = PulseTheme.colors
    val typography = PulseTheme.typography
    val monthlySavings = data.remaining.max(BigDecimal.ZERO)
    PulseCard(Modifier.fillMaxWidth(), contentPadding = PulseCardPadding.medium) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(44.dp).background(PulseBrandColors.blue.copy(alpha = 0.15f), PulseTheme.shapes.control),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.pulse_horizon_projection_arrow),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = PulseBrandColors.blue,
                    )
                }
                Column {
                    PulseText(
                        stringResource(R.string.pulse_horizon_future_projections),
                        Modifier.semantics { heading() },
                        style = typography.title1.copy(fontWeight = FontWeight.Bold, letterSpacing = typography.heading3.letterSpacing),
                    )
                    PulseText(
                        stringResource(R.string.pulse_horizon_projected_savings),
                        style = PulseTextStyle.Body2,
                        color = colors.textSecondary,
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fitsRow = maxWidth >= 540.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                val tiles: @Composable (Modifier) -> Unit = { modifier ->
                    projectionMonths.forEach { months ->
                        ProjectionTile(
                            months = months,
                            amount = data.formatMoney(monthlySavings * months.toBigDecimal()),
                            padding = if (fitsRow) 20.dp else 16.dp,
                            modifier = modifier,
                        )
                    }
                }
                if (fitsRow) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        tiles(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        tiles(Modifier.fillMaxWidth())
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(color = colors.divider)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        painterResource(R.drawable.pulse_horizon_projection_info),
                        contentDescription = null,
                        modifier = Modifier.padding(top = 2.dp).size(16.dp),
                        tint = colors.action,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        PulseText(stringResource(R.string.pulse_horizon_projection_assumptions), style = PulseTextStyle.Label)
                        PulseText(
                            stringResource(R.string.pulse_horizon_projection_assumptions_description),
                            style = PulseTextStyle.Body2,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectionTile(
    months: Int,
    amount: String,
    padding: Dp,
    modifier: Modifier,
) {
    val colors = PulseTheme.colors
    val typography = PulseTheme.typography
    Column(
        modifier
            .background(colors.surfaceMuted, PulseTheme.shapes.large)
            .semantics(mergeDescendants = true) {}
            .padding(padding),
    ) {
        PulseText(
            stringResource(R.string.pulse_horizon_projection_months, months),
            style = PulseTextStyle.Label,
            color = colors.textSecondary,
        )
        PulseText(
            amount,
            Modifier.padding(top = 4.dp),
            style = typography.heading3.copy(fontFeatureSettings = "tnum"),
        )
    }
}
