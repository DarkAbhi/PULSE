package dev.abhishekan.pulse.feature.fitness.workout.editor

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseNumberField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.feature.fitness.R
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutAction
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutGlyph
import dev.abhishekan.pulse.feature.fitness.workout.WorkoutSymbol

private const val MAXIMUM_SETS = 20

@Composable
internal fun ExerciseSetEditor(
    sets: List<SetDraft>,
    busy: Boolean,
    onAction: (WorkoutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    // Keep focus and composition identity attached to surviving rows, even when values are identical.
    var rowIds by rememberSaveable { mutableStateOf(sets.indices.toList()) }
    var nextRowId by rememberSaveable { mutableIntStateOf(sets.size) }
    val repsFocus = remember(rowIds) { rowIds.map { FocusRequester() } }
    val canRemove = sets.size > 1

    PulseCard(
        modifier.fillMaxWidth().animateContentSize(animationSpec = tween(PulseTokens.MOTION_MILLIS)),
        shape = PulseTheme.shapes.extraLarge,
        contentPadding = PulseTheme.spacing.compact,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PulseTokens.tiny),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PulseText(
                stringResource(R.string.pulse_fitness_your_sets),
                Modifier.weight(1f).semantics { heading() },
                PulseTextStyle.Title2,
            )
            PulseText(
                stringResource(R.string.pulse_fitness_editor_set_count, sets.size, MAXIMUM_SETS),
                style = PulseTextStyle.Label,
                color = PulseTheme.colors.textSecondary,
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
            val indexWidth = PulseTokens.large * fontScale
            val controlsWidth = if (canRemove) PulseTokens.target + PulseTokens.small else PulseTheme.spacing.none
            val fieldWidth = (maxWidth - indexWidth - controlsWidth - PulseTokens.small * 2) / 2
            val stacked = fieldWidth / fontScale < PulseTheme.spacing.spacious + PulseTokens.small
            val showUnitInField = stacked || fieldWidth / fontScale >= PulseTheme.spacing.spacious + PulseTokens.large
            val weightHeading =
                if (showUnitInField) R.string.pulse_fitness_editor_weight else R.string.pulse_fitness_editor_weight_kg

            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                if (!stacked) {
                    SetColumns(
                        index = { Spacer(Modifier.width(indexWidth)) },
                        fields = {
                            SetColumnHeading(R.string.pulse_fitness_editor_reps, Modifier.weight(1f))
                            SetColumnHeading(weightHeading, Modifier.weight(1f))
                        },
                        remove = { if (canRemove) Spacer(Modifier.width(PulseTokens.target)) },
                    )
                }
                sets.forEachIndexed { index, set ->
                    key(rowIds.getOrElse(index) { index }) {
                        val setNumber = index + 1
                        val weightFocus = remember { FocusRequester() }
                        val nextReps = repsFocus.getOrNull(index + 1)
                        val badge: @Composable () -> Unit = {
                            val description = stringResource(R.string.pulse_fitness_set_1_s, setNumber)
                            Box(
                                Modifier
                                    .sizeIn(minWidth = indexWidth, minHeight = PulseTokens.large)
                                    .background(PulseTheme.colors.surfaceMuted, PulseTheme.shapes.pill)
                                    .clearAndSetSemantics { contentDescription = description },
                                contentAlignment = Alignment.Center,
                            ) {
                                PulseText(setNumber.toString(), style = PulseTextStyle.Label)
                            }
                        }
                        val remove: @Composable () -> Unit = {
                            if (canRemove) {
                                PulseButton(
                                    stringResource(R.string.pulse_fitness_remove_set_1_s, setNumber),
                                    {
                                        focus.clearFocus()
                                        rowIds = rowIds.filterIndexed { rowIndex, _ -> rowIndex != index }
                                        onAction(WorkoutAction.RemoveSet(index))
                                    },
                                    enabled = !busy,
                                    style = PulseButtonStyle.Icon,
                                ) {
                                    WorkoutGlyph(
                                        WorkoutSymbol.Delete,
                                        if (busy) PulseTheme.colors.textSecondary else PulseTheme.colors.error,
                                    )
                                }
                            }
                        }
                        val reps: @Composable (Modifier) -> Unit = { fieldModifier ->
                            val requester = repsFocus.getOrNull(index)
                            SetNumberField(
                                value = set.reps,
                                onValueChange = { onAction(WorkoutAction.ChangeSet(index, it, null)) },
                                label = stringResource(R.string.pulse_fitness_reps_set_1_s, setNumber),
                                enabled = !busy,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { weightFocus.requestFocus() }),
                                modifier = if (requester == null) fieldModifier else fieldModifier.focusRequester(requester),
                            )
                        }
                        val weight: @Composable (Modifier) -> Unit = { fieldModifier ->
                            SetNumberField(
                                value = set.weight,
                                onValueChange = { onAction(WorkoutAction.ChangeSet(index, null, it)) },
                                label = stringResource(R.string.pulse_fitness_weight_kg_set_1_s, setNumber),
                                enabled = !busy,
                                keyboardOptions =
                                    KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = if (nextReps == null) ImeAction.Done else ImeAction.Next,
                                    ),
                                keyboardActions =
                                    KeyboardActions(
                                        onNext = { nextReps?.requestFocus() },
                                        onDone = {
                                            keyboard?.hide()
                                            focus.clearFocus()
                                        },
                                    ),
                                modifier = fieldModifier.focusRequester(weightFocus),
                                showUnit = showUnitInField,
                            )
                        }
                        if (stacked) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                badge()
                                remove()
                            }
                            SetColumnHeading(R.string.pulse_fitness_editor_reps)
                            reps(Modifier.fillMaxWidth())
                            SetColumnHeading(weightHeading)
                            weight(Modifier.fillMaxWidth())
                        } else {
                            SetColumns(
                                index = badge,
                                fields = {
                                    reps(Modifier.weight(1f))
                                    weight(Modifier.weight(1f))
                                },
                                remove = remove,
                            )
                        }
                    }
                }
            }
        }
        PulseButton(
            stringResource(R.string.pulse_fitness_editor_add_set),
            {
                rowIds = rowIds + nextRowId++
                onAction(WorkoutAction.AddSet)
            },
            Modifier.fillMaxWidth(),
            enabled = !busy && sets.size < MAXIMUM_SETS,
            variant = PulseButtonVariant.Secondary,
            contentPadding = PaddingValues(PulseTokens.small),
            leadingIcon = {
                WorkoutGlyph(
                    WorkoutSymbol.Add,
                    if (busy || sets.size >= MAXIMUM_SETS) PulseTheme.colors.textSecondary else PulseTheme.colors.action,
                )
            },
        )
        PulseText(
            stringResource(R.string.pulse_fitness_repetitions_1_1_000_per_set_leave_weight_blank),
            modifier = Modifier.padding(horizontal = PulseTokens.tiny),
            style = PulseTextStyle.Caption,
            color = PulseTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun SetColumns(
    index: @Composable () -> Unit,
    fields: @Composable RowScope.() -> Unit,
    remove: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        index()
        fields()
        remove()
    }
}

@Composable
private fun SetColumnHeading(
    label: Int,
    modifier: Modifier = Modifier,
) {
    PulseText(
        stringResource(label),
        modifier,
        PulseTextStyle.Overline,
        color = PulseTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SetNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier,
    showUnit: Boolean = false,
) {
    // The visible labels belong to the table. Each editor retains a set-specific TalkBack label.
    val colors = PulseTheme.colors
    PulseNumberField(
        value,
        onValueChange,
        label,
        modifier,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        suffix =
            if (showUnit) {
                {
                    PulseText(
                        stringResource(R.string.pulse_fitness_editor_kg),
                        Modifier.clearAndSetSemantics {},
                        PulseTextStyle.Label2,
                        color = colors.textSecondary,
                    )
                }
            } else {
                null
            },
    )
}
