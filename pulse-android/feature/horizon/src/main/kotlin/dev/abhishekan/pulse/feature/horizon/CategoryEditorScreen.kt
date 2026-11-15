package dev.abhishekan.pulse.feature.horizon

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText

@Composable
internal fun CategoryEditorScreen(
    draft: HorizonDraft,
    busy: Boolean,
    error: UiText?,
    backLabel: String,
    onChange: (HorizonDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    notice: @Composable () -> Unit,
    canSave: Boolean,
) {
    HorizonEditorScaffold(
        title = stringResource(draft.topBarTitle()),
        backLabel = backLabel,
        busy = busy,
    ) { width ->
        HorizonEditorForm(width, busy, canSave, PulseTokens.contentWidth, onSave, onCancel) {
            notice()
            CategoryEditor(draft, busy, error, canSave, onChange, onSave)
        }
    }
}
