package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealTime
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle

@Composable
internal fun MealEditorHeader(
    title: String,
    illustrated: Boolean,
) {
    val colors = PulseTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val showArt = illustrated && LocalDensity.current.fontScale <= 1.3f
        val artSize = if (maxWidth < 400.dp) 104.dp else 172.dp
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = PulseTheme.shapes.large, color = colors.primarySoft) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_meals),
                            null,
                            Modifier.size(24.dp),
                            tint = colors.onPrimarySoft,
                        )
                    }
                }
                PulseText(title, Modifier.semantics { heading() }, PulseTextStyle.Heading2)
                if (illustrated) {
                    PulseText(
                        stringResource(R.string.pulse_mealplan_editor_subtitle),
                        color = colors.textSecondary,
                    )
                }
            }
            if (showArt) {
                Image(
                    painterResource(R.drawable.meal_plan_hero),
                    null,
                    Modifier.size(artSize),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
internal fun MealTimeChips(
    times: List<MealTime>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (MealTime) -> Unit,
) {
    val colors = PulseTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        times.forEach { time ->
            val accent =
                when {
                    time.start.hour < 11 -> colors.primary
                    time.start.hour < 15 -> colors.coral
                    time.start.hour < 19 -> colors.error
                    else -> colors.lavender
                }
            val icon =
                when {
                    time.start.hour < 15 -> R.drawable.meal_sun
                    time.start.hour < 19 -> R.drawable.meal_sunset
                    else -> R.drawable.meal_moon
                }
            val selected = selectedId == time.id
            val background by animateColorAsState(
                if (selected) colors.primarySoft else accent.copy(alpha = 0.12f),
                label = "meal-time-color",
            )
            FilterChip(
                selected = selected,
                onClick = { onSelect(time) },
                label = { PulseText(time.name) },
                modifier = Modifier.padding(vertical = 4.dp).heightIn(min = 48.dp),
                enabled = enabled,
                leadingIcon = {
                    Icon(painterResource(icon), null, Modifier.size(24.dp), tint = accent)
                },
                trailingIcon =
                    if (selected) {
                        {
                            Icon(
                                painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_check),
                                null,
                                Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                shape = CircleShape,
                border = BorderStroke(1.dp, if (selected) colors.action else accent.copy(alpha = 0.25f)),
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = background,
                        selectedContainerColor = background,
                        labelColor = colors.textPrimary,
                        selectedLabelColor = colors.onPrimarySoft,
                    ),
            )
        }
    }
}
