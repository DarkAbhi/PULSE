package dev.abhishekan.pulse.feature.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message

@Composable
internal fun ProfileReviewCard(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val review = state.review ?: return
    PulseCard(modifier.fillMaxWidth()) {
        PulseText(review.reviewMessage, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        when (review.takeIf { state.reviewed }) {
            ProfileMutation.Name -> {
                val name = state.profile?.name ?: stringResource(R.string.pulse_profile_not_set)
                PulseText(stringResource(R.string.pulse_profile_server_name, name))
            }
            ProfileMutation.Currency -> {
                state.profile?.let {
                    PulseText(stringResource(R.string.pulse_profile_server_currency, ProfilePreference.Currency.label(it.currency)))
                }
            }
            ProfileMutation.TimeZone -> {
                state.profile?.let {
                    PulseText(stringResource(R.string.pulse_profile_server_timezone, ProfilePreference.TimeZone.label(it.timezone)))
                }
            }
            ProfileMutation.Key, ProfileMutation.Revoke ->
                state.keys.forEach { key ->
                    PulseText(
                        stringResource(R.string.pulse_profile_server_key, key.name, key.prefix),
                        style = PulseTextStyle.Label,
                    )
                }
            ProfileMutation.Password, null -> Unit
        }
        if (review.reviewsProfile) {
            state.profileError?.let { PulseErrorState(it.message, { onAction(ProfileAction.RefreshReview) }) }
        }
        if (review.reviewsKeys) {
            state.keysError?.let { PulseErrorState(it.message, { onAction(ProfileAction.RefreshReview) }) }
        }
        if (!state.passwordUncertain) {
            val reviewing = (review.reviewsProfile && state.loading) || (review.reviewsKeys && state.keysLoading)
            PulseButton(
                stringResource(R.string.pulse_profile_review),
                { onAction(ProfileAction.RefreshReview) },
                enabled = !state.busy && !reviewing,
                style = PulseButtonStyle.Text,
            )
        }
        PulseButton(
            stringResource(if (state.passwordUncertain) R.string.pulse_profile_understood else R.string.pulse_profile_reviewed),
            { onAction(ProfileAction.AcknowledgeReview) },
            enabled = state.reviewed && !state.busy,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileReviewCardPreview() {
    PulseTheme {
        ProfileReviewCard(
            ProfileUiState(
                profileLoad = ProfileLoad.Loaded(Profile(name = "Asha")),
                review = ProfileMutation.Name,
            ),
            onAction = {},
        )
    }
}
