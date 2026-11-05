package dev.abhishekan.pulse.feature.mealplan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.R as UiR

private val chapterLabels = listOf(R.string.meal_setup_chapter_you, R.string.meal_setup_chapter_food, R.string.meal_setup_chapter_rhythm)
private val chapterTitles = listOf(R.string.meal_setup_priorities, R.string.meal_setup_food, R.string.meal_setup_rhythm)
private val chapterDescriptions =
    listOf(R.string.meal_setup_priorities_detail, R.string.meal_setup_food_detail, R.string.meal_setup_rhythm_detail)
private val chapterIcons = listOf(UiR.drawable.pulse_person, UiR.drawable.pulse_meals, R.drawable.meal_clock)

/** Six question marks grouped into three meaningful chapters, with no decorative banner on phones. */
@Composable
internal fun MealSetupChapterProgress(
    step: Int,
    modifier: Modifier = Modifier,
) {
    val progressLabel = stringResource(R.string.meal_setup_progress, (step + 1).coerceAtMost(6))
    Row(
        modifier.clearAndSetSemantics {
            contentDescription = progressLabel
            progressBarRangeInfo = ProgressBarRangeInfo((step + 1).coerceAtMost(6).toFloat(), 0f..6f)
        },
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium),
    ) {
        chapterLabels.forEachIndexed { chapter, label ->
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseText(
                    stringResource(label),
                    style = PulseTextStyle.Caption,
                    color =
                        if (chapter ==
                            (step / 2).coerceAtMost(2)
                        ) {
                            PulseTheme.colors.action
                        } else {
                            PulseTheme.colors.textSecondary
                        },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                    repeat(2) { question ->
                        Box(
                            Modifier.weight(1f).height(4.dp).clip(PulseTheme.shapes.pill).background(
                                if (chapter * 2 + question <=
                                    step
                                ) {
                                    PulseTheme.colors.action
                                } else {
                                    PulseTheme.colors.surfaceStrong
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** A tablet-only editorial panel: appetite, context and a map of the user's choices. */
@Composable
internal fun MealSetupInfographic(
    step: Int,
    modifier: Modifier = Modifier,
) {
    val chapter = (step / 2).coerceAtMost(2)
    PulseCard(modifier, highlighted = true, contentPadding = 0.dp) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTokens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.large),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseText(stringResource(R.string.meal_setup_editorial_title), style = PulseTextStyle.Heading1)
                PulseText(
                    stringResource(
                        if (step == 6) {
                            R.string.meal_setup_editorial_review
                        } else {
                            when (chapter) {
                                0 -> R.string.meal_setup_editorial_you
                                1 -> R.string.meal_setup_editorial_food
                                else -> R.string.meal_setup_editorial_rhythm
                            }
                        },
                    ),
                    color = PulseTheme.colors.onPrimarySoft,
                )
            }
            Image(
                painterResource(if (chapter == 1) R.drawable.meal_onboarding_pesto else R.drawable.meal_onboarding_salad),
                null,
                Modifier.fillMaxWidth().height(200.dp).testTag("meal-setup-artwork"),
                contentScale = ContentScale.Fit,
            )
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                chapterTitles.forEachIndexed { index, title ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                        Box(
                            Modifier.size(PulseTokens.target).clip(PulseTheme.shapes.pill).background(
                                if (index ==
                                    chapter
                                ) {
                                    PulseTheme.colors.action
                                } else {
                                    PulseTheme.colors.surface.copy(alpha = 0.6f)
                                },
                            ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(
                                    if (step >=
                                        (index + 1) * 2
                                    ) {
                                        UiR.drawable.pulse_check
                                    } else {
                                        chapterIcons[index]
                                    },
                                ),
                                null,
                                Modifier.size(20.dp),
                                tint =
                                    if (index ==
                                        chapter
                                    ) {
                                        PulseTheme.colors.inverseText
                                    } else {
                                        PulseTheme.colors.action
                                    },
                            )
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.tiny)) {
                            PulseText(stringResource(title), style = PulseTextStyle.Label)
                            PulseText(
                                stringResource(chapterDescriptions[index]),
                                style = PulseTextStyle.Caption,
                                color = PulseTheme.colors.onPrimarySoft,
                            )
                        }
                    }
                }
            }
            PulseText(
                stringResource(R.string.meal_setup_editorial_flexible),
                style = PulseTextStyle.Caption,
                color = PulseTheme.colors.onPrimarySoft,
            )
        }
    }
}
