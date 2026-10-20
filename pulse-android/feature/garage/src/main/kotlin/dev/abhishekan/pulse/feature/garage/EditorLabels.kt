package dev.abhishekan.pulse.feature.garage

internal enum class EditorAction { Add, Edit, Save }

@androidx.annotation.StringRes
internal fun GarageForm.editorLabel(action: EditorAction): Int =
    when (this) {
        GarageForm.Vehicle ->
            when (action) {
                EditorAction.Add -> R.string.pulse_garage_add_vehicle
                EditorAction.Edit -> R.string.pulse_garage_edit_vehicle
                EditorAction.Save -> R.string.pulse_garage_save_vehicle
            }
        GarageForm.Fuel ->
            when (action) {
                EditorAction.Add -> R.string.pulse_garage_add_fuel
                EditorAction.Edit -> R.string.pulse_garage_edit_fuel
                EditorAction.Save -> R.string.pulse_garage_save_fuel
            }
        GarageForm.Care ->
            when (action) {
                EditorAction.Add -> R.string.pulse_garage_add_care
                EditorAction.Edit -> R.string.pulse_garage_edit_care
                EditorAction.Save -> R.string.pulse_garage_save_care
            }
        GarageForm.Pressure ->
            when (action) {
                EditorAction.Add -> R.string.pulse_garage_add_pressure
                EditorAction.Edit -> R.string.pulse_garage_edit_pressure
                EditorAction.Save -> R.string.pulse_garage_save_pressure
            }
    }
