package dev.abhishekan.pulse.feature.horizon

import androidx.annotation.StringRes

internal enum class EditorAction { Add, Edit, Save }

@StringRes
internal fun HorizonForm.editorLabel(action: EditorAction): Int =
    when (this) {
        HorizonForm.Config ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_config
                EditorAction.Edit -> R.string.pulse_horizon_edit_config
                EditorAction.Save -> R.string.pulse_horizon_save_config
            }
        HorizonForm.Budget ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_budget
                EditorAction.Edit -> R.string.pulse_horizon_edit_budget
                EditorAction.Save -> R.string.pulse_horizon_save_budget
            }
        HorizonForm.Deduction ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_deduction
                EditorAction.Edit -> R.string.pulse_horizon_edit_deduction
                EditorAction.Save -> R.string.pulse_horizon_save_deduction
            }
        HorizonForm.Transaction ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_transaction
                EditorAction.Edit -> R.string.pulse_horizon_edit_transaction
                EditorAction.Save -> R.string.pulse_horizon_save_transaction
            }
        HorizonForm.Subscription ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_subscription
                EditorAction.Edit -> R.string.pulse_horizon_edit_subscription
                EditorAction.Save -> R.string.pulse_horizon_save_subscription
            }
        HorizonForm.Category ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_category
                EditorAction.Edit -> R.string.pulse_horizon_edit_category
                EditorAction.Save -> R.string.pulse_horizon_save_category
            }
        HorizonForm.Purchase ->
            when (action) {
                EditorAction.Add -> R.string.pulse_horizon_add_purchase
                EditorAction.Edit -> R.string.pulse_horizon_edit_purchase
                EditorAction.Save -> R.string.pulse_horizon_save_purchase
            }
    }
