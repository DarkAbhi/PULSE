package dev.abhishekan.pulse.feature.horizon.presentation.categories

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun CategoryEditorScreen(
    state: HorizonEditorState,
    backLabel: String,
    onAction: (HorizonEditorAction) -> Unit,
    notice: @Composable () -> Unit,
) {
    val save = { onAction(HorizonEditorAction.Save) }
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
            onSave = save,
            onCancel = { onAction(HorizonEditorAction.Cancel) },
        ) {
            notice()
            CategoryEditor(
                state.draft,
                state.busy,
                state.error,
                state.canSave,
                onChange = { onAction(HorizonEditorAction.Change(it)) },
                onSave = save,
            )
        }
    }
}
