package dev.abhishekan.pulse.feature.horizon.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorAction
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorForm
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorScaffold
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonEditorState
import dev.abhishekan.pulse.feature.horizon.presentation.editor.topBarTitle

@Composable
internal fun HorizonSettingsEditorScreen(
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
            PulseTokens.readingWidth,
            onSave,
            onCancel = { onAction(HorizonEditorAction.Cancel) },
        ) {
            notice()
            HorizonEditor(
                state.draft,
                state.data,
                state.busy,
                state.error,
                { onAction(HorizonEditorAction.Change(it)) },
                onSave,
            )
        }
    }
}
