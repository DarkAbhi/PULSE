package dev.abhishekan.pulse.feature.horizon.presentation.editor

import dev.abhishekan.pulse.core.domain.model.HorizonOverview
import dev.abhishekan.pulse.core.ui.UiText

internal data class HorizonEditorState(
    val draft: HorizonDraft,
    val data: HorizonOverview?,
    val busy: Boolean,
    val error: UiText?,
    val reviewRequired: Boolean,
) {
    val canSave: Boolean get() = !busy && !reviewRequired && data != null
}

internal sealed interface HorizonEditorAction {
    data class Change(
        val draft: HorizonDraft,
    ) : HorizonEditorAction

    data object Save : HorizonEditorAction

    data object Cancel : HorizonEditorAction

    data object AddCategory : HorizonEditorAction

    data class DeleteObligation(
        val id: Long,
    ) : HorizonEditorAction
}
