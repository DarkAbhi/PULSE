package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import dev.abhishekan.pulse.core.domain.model.CatalogExercise
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseBottomSheet
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.feature.fitness.R

@Composable
internal fun ExerciseSelector(
    editor: ExerciseEditorState,
    query: String,
    busy: Boolean,
    onQuery: (String) -> Unit,
    onChoose: (CatalogExercise) -> Unit,
    onCustom: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    PulseBottomSheet(stringResource(R.string.pulse_fitness_editor_search_title), onDismiss, fullScreen = true) {
        val searchFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { searchFocus.requestFocus() }
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = PulseTheme.spacing.medium)
                .padding(bottom = PulseTheme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            PulseTextField(
                query,
                onQuery,
                stringResource(R.string.pulse_fitness_editor_search_placeholder),
                Modifier.fillMaxWidth().focusRequester(searchFocus),
                enabled = !busy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(onSearch = {
                        onRetry()
                        keyboard?.hide()
                        focus.clearFocus()
                    }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        PulseButton(
                            stringResource(R.string.pulse_fitness_editor_clear_search),
                            { onQuery("") },
                            style = PulseButtonStyle.Icon,
                        ) {
                            PulseText(stringResource(R.string.pulse_fitness_clear_search_glyph))
                        }
                    }
                },
            )
            val searchingWithoutResults = editor.searching && editor.candidates.isEmpty()
            val resultsScroll = rememberScrollState()
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (searchingWithoutResults) Modifier else Modifier.verticalScroll(resultsScroll))
                    .semantics { selectableGroup() },
                verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                if (editor.searching) {
                    PulseCenteredLoadingState(
                        stringResource(R.string.pulse_fitness_finding_exercises),
                        if (searchingWithoutResults) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                    )
                }
                editor.searchError?.let {
                    PulseText(it, color = PulseTheme.colors.error)
                    PulseButton(
                        stringResource(R.string.pulse_fitness_retry_catalogue_search),
                        onRetry,
                        enabled = !busy,
                        style = PulseButtonStyle.Text,
                    )
                }
                if (!editor.searching && editor.searchError == null && editor.candidates.isEmpty()) {
                    PulseText(
                        stringResource(
                            if (query.isBlank()) {
                                R.string.pulse_fitness_editor_search_hint
                            } else {
                                R.string.pulse_fitness_no_matches_yet_you_can_save_your_own_exercise
                            },
                        ),
                        Modifier.padding(vertical = PulseTokens.medium),
                        color = PulseTheme.colors.textSecondary,
                    )
                }
                editor.candidates.forEach { candidate ->
                    val selected = editor.draft.catalogId == candidate.id
                    val metadata =
                        listOfNotNull(
                            candidate.equipment?.takeIf { it.isNotBlank() },
                            candidate.muscles.takeIf { it.isNotEmpty() }?.joinToString(),
                        )
                    PulseRadioButton(
                        selected = selected,
                        onClick = { onChoose(candidate) },
                        label = candidate.name,
                        supportingText = metadata.takeIf { it.isNotEmpty() }?.joinToString(" • "),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    )
                }
            }
            if (query.isNotBlank()) {
                PulseButton(
                    stringResource(R.string.pulse_fitness_editor_custom_action, query),
                    onCustom,
                    Modifier.fillMaxWidth(),
                    enabled = !busy,
                    style = PulseButtonStyle.Text,
                )
            }
        }
    }
}
