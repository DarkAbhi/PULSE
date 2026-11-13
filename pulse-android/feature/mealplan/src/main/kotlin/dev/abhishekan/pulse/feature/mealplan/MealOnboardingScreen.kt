package dev.abhishekan.pulse.feature.mealplan

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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.abhishekan.pulse.core.domain.model.MealPreferenceChoices
import dev.abhishekan.pulse.core.domain.model.MealPreferenceTime
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import dev.abhishekan.pulse.core.ui.message
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
private val reviewLabels =
    listOf(
        R.string.meal_review_goal,
        R.string.meal_review_barriers,
        R.string.meal_review_diet,
        R.string.meal_review_avoidances,
        R.string.meal_review_meals,
        R.string.meal_review_times,
    )
private val choiceLabels =
    mapOf(
        "feel_satisfied" to R.string.meal_goal_satisfied,
        "food_calm" to R.string.meal_goal_calm,
        "nourish_without_dieting" to R.string.meal_goal_nourish,
        "hungry_again" to R.string.meal_barrier_hungry,
        "too_tired" to R.string.meal_barrier_tired,
        "unsure_what_to_add" to R.string.meal_barrier_unsure,
        "food_advice_overwhelming" to R.string.meal_barrier_overwhelmed,
        "no_preference" to R.string.meal_diet_none,
        "vegetarian" to R.string.meal_diet_vegetarian,
        "vegan" to R.string.meal_diet_vegan,
        "pescatarian" to R.string.meal_diet_pescatarian,
        "shellfish" to R.string.meal_avoid_shellfish,
        "fish" to R.string.meal_avoid_fish,
        "peanuts" to R.string.meal_avoid_peanuts,
        "tree_nuts" to R.string.meal_avoid_tree_nuts,
        "dairy" to R.string.meal_avoid_dairy,
        "eggs" to R.string.meal_avoid_eggs,
        "gluten" to R.string.meal_avoid_gluten,
        "soy" to R.string.meal_avoid_soy,
        "sesame" to R.string.meal_avoid_sesame,
        "pork" to R.string.meal_avoid_pork,
        "beef" to R.string.meal_avoid_beef,
        "poultry" to R.string.meal_avoid_poultry,
        "mushrooms" to R.string.meal_avoid_mushrooms,
        "onion_garlic" to R.string.meal_avoid_onion,
        "spicy_food" to R.string.meal_avoid_spicy,
        "alcohol" to R.string.meal_avoid_alcohol,
        "breakfast" to R.string.meal_setup_breakfast,
        "lunch" to R.string.meal_setup_lunch,
        "dinner" to R.string.meal_setup_dinner,
    )

@Composable
private fun choiceLabel(code: String) = choiceLabels[code]?.let { stringResource(it) } ?: code

@Composable
internal fun MealOnboardingScreen(
    state: MealOnboardingUiState,
    vm: MealOnboardingViewModel,
) {
    BackHandler(state.saving || (state.showJourney && (state.step > 0 || state.editing))) { if (!state.saving) vm.back() }
    Box(Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).imePadding().padding(PulseTokens.medium)) {
        if (state.record == null) {
            if (state.error != null) {
                PulseErrorState(state.error.message, vm::refresh)
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
                        MealSetupQuestion(state, vm)
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
                        val leave = PulseFormAction(
                            stringResource(if (state.editing) CoreUiR.string.pulse_ui_cancel else CoreUiR.string.pulse_ui_skip),
                            { if (state.editing) vm.cancelEditing() else vm.skip() },
                            enabled = !state.saving,
                        )
                        val back = if (state.step > 0) {
                            PulseFormAction(
                                stringResource(CoreUiR.string.pulse_ui_back),
                                vm::back,
                                enabled = !state.saving,
                                modifier = Modifier.testTag("meal-setup-back"),
                            )
                        } else {
                            null
                        }
                        PulseFormActions(
                            primary = PulseFormAction(
                                stringResource(
                                    when {
                                        state.step == 6 -> CoreUiR.string.pulse_ui_save
                                        state.returnToReview -> CoreUiR.string.pulse_ui_review
                                        else -> CoreUiR.string.pulse_ui_next
                                    },
                                ),
                                { if (state.step == 6) vm.save() else vm.next() },
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

@Composable
private fun MealSetupQuestion(
    state: MealOnboardingUiState,
    vm: MealOnboardingViewModel,
) {
    val p = state.draft
    var timeKind by remember { mutableStateOf<String?>(null) }
    val timeDrafts = remember { mutableMapOf<String, LocalTime>() }

    fun toggle(
        values: List<String>,
        code: String,
    ) = if (code in values) values - code else values + code
    Column(
        Modifier.fillMaxWidth().then(
            if (state.step == 0 ||
                state.step == 2
            ) {
                Modifier.selectableGroup()
            } else {
                Modifier
            },
        ),
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        when (state.step) {
            0, 2 ->
                (if (state.step == 0) MealPreferenceChoices.goals else MealPreferenceChoices.diets).forEach { code ->
                    val selected = code == if (state.step == 0) p.goal else p.dietaryPattern
                    PulseCard(Modifier.fillMaxWidth(), highlighted = selected, contentPadding = PulseTokens.small) {
                        PulseRadioButton(selected, {
                            vm.update(
                                if (state.step ==
                                    0
                                ) {
                                    p.copy(goal = code)
                                } else {
                                    p.copy(dietaryPattern = code)
                                },
                            )
                        }, choiceLabel(code), Modifier.fillMaxWidth(), enabled = !state.saving)
                    }
                }
            1, 3 ->
                (if (state.step == 1) MealPreferenceChoices.barriers else MealPreferenceChoices.avoidances).forEach { code ->
                    val checked = code in if (state.step == 1) p.barriers else p.foodAvoidances
                    PulseCard(Modifier.fillMaxWidth(), highlighted = checked, contentPadding = PulseTokens.small) {
                        PulseCheckbox(checked, {
                            vm.update(
                                if (state.step ==
                                    1
                                ) {
                                    p.copy(barriers = toggle(p.barriers, code))
                                } else {
                                    p.copy(foodAvoidances = toggle(p.foodAvoidances, code))
                                },
                            )
                        }, choiceLabel(code), Modifier.fillMaxWidth(), enabled = !state.saving)
                    }
                }
            4 ->
                MealPreferenceChoices.meals.forEach { kind ->
                    PulseCard(Modifier.fillMaxWidth(), contentPadding = PulseTokens.medium) {
                        PulseSwitch(p.meals.any { it.kind == kind }, { checked ->
                            p.meals.forEach { timeDrafts[it.kind] = it.time }
                            val meals =
                                if (checked) {
                                    p.meals + MealPreferenceTime(kind, timeDrafts[kind] ?: MealPreferencesDefaults.time(kind))
                                } else {
                                    p.meals.filterNot {
                                        it.kind ==
                                            kind
                                    }
                                }
                            vm.update(p.copy(meals = meals.sortedBy { MealPreferenceChoices.meals.indexOf(it.kind) }))
                        }, choiceLabel(kind), Modifier.fillMaxWidth(), enabled = !state.saving, switchAtEnd = true)
                    }
                }
            5 -> {
                PulseText(stringResource(R.string.meal_setup_timezone, UserTimeZone.zone.id), style = PulseTextStyle.Caption)
                p.meals.forEach { meal ->
                    PulseButton(
                        stringResource(
                            R.string.meal_setup_time_label,
                            choiceLabel(meal.kind),
                            meal.time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        ),
                        {
                            timeKind =
                                meal.kind
                        },
                        Modifier.fillMaxWidth(),
                        enabled = !state.saving,
                        variant = PulseButtonVariant.Secondary,
                    )
                }
            }
            6 -> {
                val empty = stringResource(R.string.meal_setup_none)
                val summaries =
                    listOf(
                        choiceLabel(p.goal),
                        p.barriers
                            .map {
                                choiceLabel(it)
                            }.joinToString()
                            .ifEmpty {
                                empty
                            },
                        choiceLabel(p.dietaryPattern),
                        p.foodAvoidances
                            .map {
                                choiceLabel(it)
                            }.joinToString()
                            .ifEmpty {
                                empty
                            },
                        p.meals
                            .map {
                                choiceLabel(it.kind)
                            }.joinToString(),
                        p.meals
                            .map {
                                "${choiceLabel(
                                    it.kind,
                                )}: ${it.time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                            }.joinToString(),
                    )
                summaries.forEachIndexed { index, summary ->
                    PulseCard(Modifier.fillMaxWidth()) {
                        val label = stringResource(reviewLabels[index])
                        val editLabel = stringResource(R.string.meal_setup_edit, label)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                                PulseText(label, style = PulseTextStyle.Label, color = PulseTheme.colors.textSecondary)
                                PulseText(summary)
                            }
                            PulseButton(
                                stringResource(R.string.meal_setup_edit_button),
                                { vm.reviewStep(index) },
                                Modifier.semantics {
                                    contentDescription =
                                        editLabel
                                },
                                enabled = !state.saving,
                                style = PulseButtonStyle.Text,
                            )
                        }
                    }
                }
            }
        }
    }
    timeKind?.let { kind ->
        PulseTimePickerDialog(p.meals.first { it.kind == kind }.time, { time ->
            vm.update(
                p.copy(
                    meals =
                        p.meals.map {
                            if (it.kind ==
                                kind
                            ) {
                                it.copy(time = time)
                            } else {
                                it
                            }
                        },
                ),
            )
            ; timeKind = null
        }, { timeKind = null })
    }
}

private object MealPreferencesDefaults {
    fun time(kind: String): LocalTime =
        when (kind) {
            "breakfast" -> LocalTime.of(9, 0)
            "lunch" -> LocalTime.of(12, 30)
            else -> LocalTime.of(19, 30)
        }
}
