package dev.abhishekan.pulse.feature.mealplan.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.MealRecipe
import dev.abhishekan.pulse.core.domain.model.MealRecipePage
import dev.abhishekan.pulse.core.domain.model.Outcome
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.mealplan.R
import kotlinx.coroutines.delay
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun MealRecipePicker(
    searchRecipes: suspend (String, Int) -> Outcome<MealRecipePage>,
    onSelect: (MealRecipe) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var retry by remember { mutableIntStateOf(0) }
    var result by remember(search, page, retry) { mutableStateOf<Outcome<MealRecipePage>?>(null) }
    LaunchedEffect(search, page, retry) {
        if (search.isNotBlank()) delay(250)
        result = searchRecipes(search, page)
    }

    PulseDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            PulseButton(stringResource(CoreUiR.string.pulse_ui_cancel), onDismiss, style = PulseButtonStyle.Text)
        },
        title = { PulseText(stringResource(R.string.pulse_mealplan_recipe_catalog), style = PulseTextStyle.Title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                PulseTextField(
                    value = search,
                    onValueChange = {
                        if (it.codePointCount(0, it.length) <= 100 && '\u0000' !in it) {
                            search = it
                            page = 1
                        }
                    },
                    label = stringResource(R.string.pulse_mealplan_search_recipes),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
                when (val response = result) {
                    null -> PulseText(stringResource(R.string.pulse_mealplan_loading_recipes))
                    is Outcome.Error -> {
                        PulseText(stringResource(R.string.pulse_mealplan_recipes_unavailable))
                        PulseButton(
                            stringResource(R.string.pulse_mealplan_retry_recipes),
                            { retry++ },
                            style = PulseButtonStyle.Text,
                        )
                    }
                    is Outcome.Success -> {
                        val recipes = response.value.recipes
                        if (recipes.isEmpty()) {
                            PulseText(stringResource(R.string.pulse_mealplan_no_recipes))
                        } else {
                            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 320.dp).fillMaxWidth()) {
                                items(recipes, key = MealRecipe::id) { recipe ->
                                    RecipeRow(recipe) { onSelect(recipe) }
                                }
                            }
                        }
                        if (page > 1 || response.value.hasMore) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PulseButton(
                                    stringResource(R.string.pulse_mealplan_previous_recipe_page),
                                    { page-- },
                                    enabled = page > 1,
                                    style = PulseButtonStyle.Text,
                                )
                                PulseText(stringResource(R.string.pulse_mealplan_recipe_page, page))
                                PulseButton(
                                    stringResource(R.string.pulse_mealplan_next_recipe_page),
                                    { page++ },
                                    enabled = response.value.hasMore && page < 10_000,
                                    style = PulseButtonStyle.Text,
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun RecipeRow(
    recipe: MealRecipe,
    onSelect: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = PulseTokens.target)
            .clickable(role = Role.Button, onClick = onSelect)
            .padding(vertical = PulseTokens.small),
        verticalArrangement = Arrangement.Center,
    ) {
        PulseText(recipe.name, style = PulseTextStyle.Label)
        val minutes = recipe.readyInMinutes
        val preparation =
            when {
                recipe.readyToEat -> stringResource(R.string.pulse_mealplan_recipe_ready)
                minutes != null -> stringResource(R.string.pulse_mealplan_recipe_minutes, minutes)
                else -> ""
            }
        val metadata = listOf(recipe.dishType, preparation).filter(String::isNotBlank).joinToString(" · ")
        if (metadata.isNotEmpty()) {
            PulseText(metadata, style = PulseTextStyle.Body3, color = PulseTheme.colors.textSecondary)
        }
    }
}
