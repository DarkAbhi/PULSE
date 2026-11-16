package dev.abhishekan.pulse.feature.mealplan.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.abhishekan.pulse.core.domain.model.MealPreferenceChoices
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseSwitch
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTimePickerDialog
import dev.abhishekan.pulse.feature.mealplan.R
import dev.abhishekan.pulse.feature.mealplan.shared.mealClock

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
        "evening_snacks" to R.string.meal_setup_evening_snacks,
        "dinner" to R.string.meal_setup_dinner,
    )

@Composable
private fun choiceLabel(code: String) = choiceLabels[code]?.let { stringResource(it) } ?: code

@Composable
internal fun MealSetupQuestion(
    state: MealOnboardingUiState,
    onAction: (MealOnboardingAction) -> Unit,
) {
    val p = state.draft
    var timeKind by rememberSaveable(state.step) { mutableStateOf<String?>(null) }

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
                            onAction(
                                if (state.step == 0) MealOnboardingAction.SelectGoal(code) else MealOnboardingAction.SelectDiet(code),
                            )
                        }, choiceLabel(code), Modifier.fillMaxWidth(), enabled = !state.saving)
                    }
                }
            1, 3 ->
                (if (state.step == 1) MealPreferenceChoices.barriers else MealPreferenceChoices.avoidances).forEach { code ->
                    val checked = code in if (state.step == 1) p.barriers else p.foodAvoidances
                    PulseCard(Modifier.fillMaxWidth(), highlighted = checked, contentPadding = PulseTokens.small) {
                        PulseCheckbox(checked, {
                            val action =
                                if (state.step == 1) {
                                    MealOnboardingAction.ToggleBarrier(code)
                                } else {
                                    MealOnboardingAction.ToggleAvoidance(code)
                                }
                            onAction(action)
                        }, choiceLabel(code), Modifier.fillMaxWidth(), enabled = !state.saving)
                    }
                }
            4 -> {
                if (state.record?.preferences?.planningSlotsConfigured == false && !p.planningSlotsConfigured) {
                    PulseText(stringResource(R.string.meal_setup_existing_planning_slots), style = PulseTextStyle.Caption)
                }
                MealPreferenceChoices.meals.forEach { kind ->
                    PulseCard(Modifier.fillMaxWidth(), contentPadding = PulseTokens.medium) {
                        PulseSwitch(p.meals.any { it.kind == kind }, { checked ->
                            onAction(MealOnboardingAction.SetMealEnabled(kind, checked))
                        }, choiceLabel(kind), Modifier.fillMaxWidth(), enabled = !state.saving, switchAtEnd = true)
                    }
                }
            }
            5 -> {
                PulseText(stringResource(R.string.meal_setup_timezone, UserTimeZone.zone.id), style = PulseTextStyle.Caption)
                p.meals.forEach { meal ->
                    PulseButton(
                        stringResource(
                            R.string.meal_setup_time_label,
                            choiceLabel(meal.kind),
                            meal.time.format(mealClock),
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
                                )}: ${it.time.format(mealClock)}"
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
                                { onAction(MealOnboardingAction.ReviewStep(index)) },
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
    p.meals.firstOrNull { state.step == 5 && it.kind == timeKind }?.let { meal ->
        PulseTimePickerDialog(
            meal.time,
            { time ->
                onAction(MealOnboardingAction.SetMealTime(meal.kind, time))
                timeKind = null
            },
            { timeKind = null },
        )
    }
}
