package dev.abhishekan.pulse.feature.mealplan

import dev.abhishekan.pulse.core.ui.UiText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.*
import dev.abhishekan.pulse.core.ui.*
import dev.abhishekan.pulse.core.ui.components.*
import java.time.*

private val day = LocalDate.of(2026,10,17)
private val fixtures = listOf(PlannedMeal(1, day, "Roasted vegetables & rice", 1, "Lunch", LocalTime.NOON, LocalTime.of(14,30), false),
    PlannedMeal(2, day, "Fruit & yogurt", null, null, null, null, true))
@Preview(name="Phone",widthDp=360,heightDp=800)
@Preview(name="Large text",widthDp=360,heightDp=800,fontScale=2f)
@Composable private fun PlannerLight() { PulseTheme(false) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PulseTokens.medium),verticalArrangement=Arrangement.spacedBy(PulseTokens.medium)) {
    PulseCard(Modifier.fillMaxWidth(),highlighted=true) {PulseText("A little care for today",style=PulseTextStyle.Display);PulseText("1 / 2",style=PulseTextStyle.Metric);PulseText("Meals consumed · selected day")}
    MealAgenda(day,day,fixtures,true,{},{},{},{})
} } }
@Preview(name="Dark weekly board",widthDp=1280,heightDp=900)
@Composable private fun PlannerDark() { PulseTheme(true) { Row(Modifier.fillMaxSize().padding(PulseTokens.cardPadding),horizontalArrangement=Arrangement.spacedBy(PulseTokens.small)) {
    MealWeekBoard((0L..6L).map(day::plusDays),day,day,fixtures,true,{})
} } }
@Preview(name="Saving form",widthDp=600,heightDp=700)
@Composable private fun SavingMeal() { PulseTheme(false) { MealEditor(MealDraft(name="Soup",date=day.toString()),emptyList(),true,false,null,{},{},{}) } }
@Preview(name="Error form",widthDp=360,heightDp=800)
@Composable private fun InvalidMealTime() { PulseTheme(true) { MealEditor(MealDraft(timeForm=true,name="Snack"),emptyList(),false,false,UiText.Literal("Enter a name and valid times."),{},{},{}) } }
