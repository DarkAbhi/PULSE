package dev.abhishekan.pulse.feature.horizon

import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.uiText

internal enum class EditorAction { Add, Edit, Save }
internal fun HorizonForm.editorLabel(action: EditorAction): UiText = when (this) {
    HorizonForm.Config -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_config); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_config); EditorAction.Save -> uiText(R.string.pulse_horizon_save_config) }
    HorizonForm.Budget -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_budget); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_budget); EditorAction.Save -> uiText(R.string.pulse_horizon_save_budget) }
    HorizonForm.Deduction -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_deduction); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_deduction); EditorAction.Save -> uiText(R.string.pulse_horizon_save_deduction) }
    HorizonForm.Transaction -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_transaction); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_transaction); EditorAction.Save -> uiText(R.string.pulse_horizon_save_transaction) }
    HorizonForm.Subscription -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_subscription); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_subscription); EditorAction.Save -> uiText(R.string.pulse_horizon_save_subscription) }
    HorizonForm.Category -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_category); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_category); EditorAction.Save -> uiText(R.string.pulse_horizon_save_category) }
    HorizonForm.Purchase -> when (action) { EditorAction.Add -> uiText(R.string.pulse_horizon_add_purchase); EditorAction.Edit -> uiText(R.string.pulse_horizon_edit_purchase); EditorAction.Save -> uiText(R.string.pulse_horizon_save_purchase) }
}
