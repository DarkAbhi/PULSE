package dev.abhishekan.pulse.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.R as CoreUiR

internal sealed interface ProfileDialog {
    enum class Fixed : ProfileDialog { Discard, Logout, Currency, TimeZone }

    data class Revoke(
        val key: APIKey,
    ) : ProfileDialog
}

internal val ProfileDialogSaver =
    Saver<ProfileDialog?, String>(
        save = { (it as? ProfileDialog.Fixed)?.name },
        restore = { ProfileDialog.Fixed.valueOf(it) },
    )

@Composable
internal fun ProfileDialogs(
    dialog: ProfileDialog?,
    state: ProfileUiState,
    otherEdits: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onAction: (ProfileAction) -> Unit,
) {
    if (state.token != null) return
    when (dialog) {
        ProfileDialog.Fixed.Discard ->
            PulseDialog(
                stringResource(R.string.pulse_profile_discard_title),
                stringResource(R.string.pulse_profile_discard_body),
                stringResource(R.string.pulse_profile_discard),
                {
                    onDismiss()
                    onAction(ProfileAction.CloseEditor)
                },
                onDismiss,
            )
        ProfileDialog.Fixed.Logout -> {
            val hasEdits = otherEdits || state.editorDirty
            PulseDialog(
                stringResource(if (hasEdits) CoreUiR.string.pulse_ui_logout_edits_title else R.string.pulse_profile_logout_title),
                stringResource(if (hasEdits) R.string.pulse_profile_logout_edits else R.string.pulse_profile_logout_body),
                stringResource(R.string.pulse_profile_logout),
                {
                    onDismiss()
                    onAction(ProfileAction.ClearSecrets)
                    onLogout()
                },
                onDismiss,
            )
        }
        is ProfileDialog.Revoke ->
            PulseDialog(
                stringResource(R.string.pulse_profile_revoke_title),
                stringResource(R.string.pulse_profile_revoke_body, dialog.key.name),
                stringResource(R.string.pulse_profile_revoke, dialog.key.name),
                {
                    onDismiss()
                    onAction(ProfileAction.RevokeKey(dialog.key.id))
                },
                onDismiss,
            )
        ProfileDialog.Fixed.Currency, ProfileDialog.Fixed.TimeZone -> {
            val preference =
                when (dialog) {
                    ProfileDialog.Fixed.Currency -> ProfilePreference.Currency
                    else -> ProfilePreference.TimeZone
                }
            key(preference) {
                ProfilePreferenceDialog(
                    preference = preference,
                    current = preference.value(state),
                    canSave = state.canEditPreferences,
                    onDismiss = onDismiss,
                    onSave = { value ->
                        onDismiss()
                        onAction(
                            when (preference) {
                                ProfilePreference.Currency -> ProfileAction.SaveCurrency(value)
                                ProfilePreference.TimeZone -> ProfileAction.SaveTimeZone(value)
                            },
                        )
                    },
                )
            }
        }
        null -> Unit
    }
}

@Preview
@Composable
private fun ProfileDialogsPreview() {
    PulseTheme {
        ProfileDialogs(ProfileDialog.Fixed.Logout, ProfileUiState(), false, {}, {}, {})
    }
}
