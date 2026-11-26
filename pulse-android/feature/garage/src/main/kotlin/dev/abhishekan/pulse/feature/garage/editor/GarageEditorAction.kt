package dev.abhishekan.pulse.feature.garage.editor

internal sealed interface GarageEditorAction {
    data object Refresh : GarageEditorAction

    data object ReviewRecords : GarageEditorAction

    data object Discard : GarageEditorAction

    data object Save : GarageEditorAction

    data object RetryCatalog : GarageEditorAction

    data object ConsumeMessage : GarageEditorAction

    data class UpdateDraft(
        val draft: GarageDraft,
    ) : GarageEditorAction
}
