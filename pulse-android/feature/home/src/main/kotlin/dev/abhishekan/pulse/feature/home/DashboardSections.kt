package dev.abhishekan.pulse.feature.home
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.Horizon
import dev.abhishekan.pulse.core.domain.model.WorkoutToday
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import java.math.BigDecimal
import dev.abhishekan.pulse.core.ui.R as UiR

internal fun formatMoney(
    currency: String,
    amount: BigDecimal,
): UiText =
    uiText(
        R.string.pulse_home_money,
        currency,
        amount.setScale(maxOf(2, amount.scale())).toPlainString(),
    )

@Composable
internal fun HorizonCard(
    section: Section<Horizon>,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    // The yellow illustration card keeps dark ink in either appearance.
    PulseTheme(false) {
        IllustratedDashboardCard(
            label = stringResource(R.string.pulse_home_financial_horizon),
            illustration = R.drawable.dashboard_cat,
            icon = UiR.drawable.pulse_home,
            background = Color(0xFFFFE58C),
            accent = Color(0xFFFFF5CE),
            action = stringResource(R.string.pulse_home_keep_going),
            onOpen = onOpen,
        ) {
            when (section) {
                Section.Loading -> PulseLoadingState(stringResource(R.string.pulse_home_loading_your_horizon))
                Section.Empty ->
                    PulseEmptyState(
                        stringResource(R.string.pulse_home_no_summary_yet),
                        stringResource(R.string.pulse_home_your_financial_summary_will_appear_here_when_available),
                    )
                is Section.Error -> PulseErrorState(section.failure.message, onRetry)
                is Section.Content -> {
                    PulseText(stringResource(R.string.pulse_home_your_room_to_move), style = PulseTextStyle.Title)
                    PulseText(
                        formatMoney(section.value.currency, section.value.remainingAmount).asString(),
                        style = PulseTextStyle.Metric,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkoutCard(
    section: Section<WorkoutToday>,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
) {
    IllustratedDashboardCard(
        label = stringResource(R.string.pulse_home_fitness_today),
        illustration = R.drawable.dashboard_fitness,
        icon = UiR.drawable.pulse_fitness,
        background = PulseTheme.colors.surface,
        accent = PulseTheme.colors.successSoft,
        action =
            stringResource(
                if (section is Section.Content) R.string.pulse_home_view_fitness else R.string.pulse_home_get_moving,
            ),
        onOpen = onOpen,
    ) {
        PulseText(
            stringResource(R.string.pulse_home_fitness_today),
            style = PulseTextStyle.Label,
            color = PulseTheme.colors.textSecondary,
        )
        when (section) {
            Section.Loading -> PulseLoadingState(stringResource(R.string.pulse_home_loading_today_s_workout))
            Section.Empty ->
                PulseText(
                    stringResource(R.string.pulse_home_no_workout_recorded_today),
                    style = PulseTextStyle.Title,
                )
            is Section.Error -> PulseErrorState(section.failure.message, onRetry)
            is Section.Content -> PulseText(stringResource(R.string.pulse_home_workout_recorded), style = PulseTextStyle.Title)
        }
    }
}

@Composable
private fun IllustratedDashboardCard(
    label: String,
    @DrawableRes illustration: Int,
    @DrawableRes icon: Int,
    background: Color,
    accent: Color,
    action: String,
    onOpen: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onOpen),
        color = background,
        contentColor = PulseTheme.colors.textPrimary,
    ) {
        Column(
            Modifier.padding(PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            Box(
                Modifier.size(48.dp).background(accent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, Modifier.size(24.dp))
            }
            content()
            Spacer(Modifier.weight(1f))
            // A separate art row keeps long amounts and large text clear of the illustration.
            Row(
                Modifier.fillMaxWidth().heightIn(min = 112.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier
                        .background(accent, RoundedCornerShape(24.dp))
                        .heightIn(min = PulseTokens.target)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PulseText(action, Modifier.weight(1f, fill = false), PulseTextStyle.Label)
                    Icon(painterResource(UiR.drawable.pulse_back), null, Modifier.size(16.dp).rotate(180f))
                }
                Image(
                    painterResource(illustration),
                    contentDescription = null,
                    modifier = Modifier.weight(1f).heightIn(max = 112.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}
