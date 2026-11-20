package dev.abhishekan.pulse.feature.mealplan.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.core.ui.R as CoreUiR

private val questionTitles =
    listOf(
        R.string.meal_setup_goal_title,
        R.string.meal_setup_barriers_title,
        R.string.meal_setup_diet_title,
        R.string.meal_setup_avoid_title,
        R.string.meal_setup_meals_title,
        R.string.meal_setup_times_title,
    )
private val questionDescriptions =
    listOf(
        R.string.meal_setup_goal_description,
        R.string.meal_setup_barriers_description,
        R.string.meal_setup_diet_description,
        R.string.meal_setup_avoid_description,
        R.string.meal_setup_meals_description,
        R.string.meal_setup_times_description,
    )

@Composable
internal fun MealOnboardingScreen(
    state: MealOnboardingUiState,
    onAction: (MealOnboardingAction) -> Unit,
    active: Boolean,
    customMealTimes: @Composable () -> Unit = {},
) {
    BackHandler(active && (state.saving || (state.showJourney && (state.step > 0 || state.editing)))) {
        if (!state.saving) onAction(MealOnboardingAction.Back)
    }
    Box(
        Modifier
            .fillMaxSize()
            .padding(LocalPulseContentPadding.current)
            .imePadding()
            .padding(PulseTokens.medium),
    ) {
        if (state.record == null) {
            if (state.error != null) {
                PulseErrorState(state.error.message, { onAction(MealOnboardingAction.Refresh) })
            } else {
                PulseCenteredLoadingState(stringResource(R.string.meal_setup_loading))
            }
            return@Box
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.large)) {
                if (wide) MealSetupInfographic(state.step, Modifier.weight(0.8f).fillMaxHeight())
                Column(Modifier.weight(1f).widthIn(max = PulseTokens.readingWidth).fillMaxSize()) {
                    val progressLabel = stringResource(R.string.meal_setup_progress, (state.step + 1).coerceAtMost(6))
                    PulseText(
                        stringResource(
                            if (state.step ==
                                6
                            ) {
                                R.string.meal_setup_review_counter
                            } else {
                                R.string.meal_setup_counter
                            },
                            state.step + 1,
                        ),
                        Modifier.fillMaxWidth().semantics {
                            contentDescription = progressLabel
                            liveRegion = LiveRegionMode.Polite
                        },
                        style = PulseTextStyle.Label,
                        color = PulseTheme.colors.textSecondary,
                    )
                    MealSetupChapterProgress(
                        state.step,
                        Modifier.fillMaxWidth().padding(top = PulseTokens.small, bottom = PulseTokens.medium),
                    )
                    val scroll = rememberScrollState()
                    LaunchedEffect(state.step) { scroll.scrollTo(0) }
                    Column(Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            PulseText(
                                stringResource(if (state.step == 6) R.string.meal_setup_review_title else questionTitles[state.step]),
                                Modifier.semantics { heading() },
                                style = if (wide) PulseTextStyle.Heading1 else PulseTextStyle.Heading2,
                            )
                            PulseText(
                                stringResource(
                                    if (state.step ==
                                        6
                                    ) {
                                        R.string.meal_setup_review_description
                                    } else {
                                        questionDescriptions[state.step]
                                    },
                                ),
                                style = PulseTextStyle.Body2,
                                color = PulseTheme.colors.textSecondary,
                            )
                        }
                        MealSetupQuestion(state, onAction)
                        if (state.step == 4) customMealTimes()
                        state.invalidStep?.let {
                            PulseText(
                                stringResource(
                                    when (it) {
                                        0 -> R.string.meal_setup_goal_error
                                        2 -> R.string.meal_setup_diet_error
                                        4 -> R.string.meal_setup_meals_error
                                        else -> R.string.meal_setup_invalid
                                    },
                                ),
                                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                                color = PulseTheme.colors.error,
                            )
                        }
                        state.error?.let {
                            PulseText(
                                it.message.asString(),
                                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                                color = PulseTheme.colors.error,
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(top = PulseTokens.medium),
                        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
                    ) {
                        val leave =
                            PulseFormAction(
                                stringResource(if (state.editing) CoreUiR.string.pulse_ui_cancel else CoreUiR.string.pulse_ui_skip),
                                { onAction(if (state.editing) MealOnboardingAction.CancelEditing else MealOnboardingAction.Skip) },
                                enabled = !state.saving,
                            )
                        val back =
                            if (state.step > 0) {
                                PulseFormAction(
                                    stringResource(CoreUiR.string.pulse_ui_back),
                                    { onAction(MealOnboardingAction.Back) },
                                    enabled = !state.saving,
                                    modifier = Modifier.testTag("meal-setup-back"),
                                )
                            } else {
                                null
                            }
                        PulseFormActions(
                            primary =
                                PulseFormAction(
                                    stringResource(
                                        when {
                                            state.step == 6 -> CoreUiR.string.pulse_ui_save
                                            state.returnToReview -> CoreUiR.string.pulse_ui_review
                                            else -> CoreUiR.string.pulse_ui_next
                                        },
                                    ),
                                    { onAction(if (state.step == 6) MealOnboardingAction.Save else MealOnboardingAction.Next) },
                                    enabled = !state.saving,
                                    loading = state.saving,
                                ),
                            secondary = back ?: leave,
                            additional = if (back != null) leave else null,
                        )
                    }
                }
            }
        }
    }
}
