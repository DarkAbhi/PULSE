package dev.abhishekan.pulse.feature.profile

internal sealed interface ProfileAction {
    data object Refresh : ProfileAction

    data object RefreshProfile : ProfileAction

    data object RefreshKeys : ProfileAction

    data object RefreshReview : ProfileAction

    data object OpenInitialNameEditor : ProfileAction

    data class Edit(
        val editor: AccountEditor,
    ) : ProfileAction

    data class ChangeName(
        val value: String,
    ) : ProfileAction

    data class ChangeKeyName(
        val value: String,
    ) : ProfileAction

    class ChangePasswordField(
        val field: PasswordField,
        val value: String,
    ) : ProfileAction

    data object SaveEditor : ProfileAction

    data object CloseEditor : ProfileAction

    data class SaveCurrency(
        val value: String,
    ) : ProfileAction

    data class SaveTimeZone(
        val value: String,
    ) : ProfileAction

    data class RevokeKey(
        val id: Long,
    ) : ProfileAction

    data object AcknowledgeReview : ProfileAction

    data object ClearSecrets : ProfileAction

    data object CloseToken : ProfileAction

    data class TokenCopied(
        val success: Boolean,
    ) : ProfileAction
}
