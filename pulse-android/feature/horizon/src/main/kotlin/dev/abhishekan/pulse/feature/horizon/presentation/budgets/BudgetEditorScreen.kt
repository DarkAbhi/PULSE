package dev.abhishekan.pulse.feature.horizon.presentation.budgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun BudgetEditorScreen(
    state: HorizonEditorState,
    backLabel: String,
    onAction: (HorizonEditorAction) -> Unit,
    notice: @Composable () -> Unit,
) {
    val onSave = { onAction(HorizonEditorAction.Save) }
    HorizonEditorScaffold(
        title = stringResource(state.draft.topBarTitle()),
        backLabel = backLabel,
        busy = state.busy,
    ) { width ->
        HorizonEditorForm(
            width,
            state.busy,
            state.canSave,
            PulseTokens.contentWidth,
            onSave,
            onCancel = { onAction(HorizonEditorAction.Cancel) },
        ) {
            notice()
            BudgetEditor(
                state.draft,
                state.busy,
                state.error,
                LocalConfiguration.current.smallestScreenWidthDp >= 600 && width != PulseWindowSize.Compact,
                state.canSave,
                { onAction(HorizonEditorAction.Change(it)) },
                onSave,
            )
        }
    }
}
