package dev.abhishekan.pulse.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.Profile
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.label
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.patterns.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.patterns.PulseErrorState
import dev.abhishekan.pulse.core.ui.patterns.PulseFormAction
import dev.abhishekan.pulse.core.ui.patterns.PulseFormActions
import dev.abhishekan.pulse.core.ui.patterns.PulseFormLayout
import dev.abhishekan.pulse.core.ui.rememberPullRefreshTracker
import java.time.ZoneId
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    username: String,
    theme: ThemePreference,
    appearanceError: UiText?,
    loggingOut: Boolean,
    otherEdits: Boolean,
    timeZone: ZoneId,
    onTheme: (ThemePreference) -> Unit,
    onLogout: () -> Unit,
    onAction: (ProfileAction) -> Unit,
    dashboardSettings: @Composable () -> Unit = {},
    debugContent: @Composable () -> Unit,
) {
    var dialog by rememberSaveable(stateSaver = ProfileDialogSaver) { mutableStateOf<ProfileDialog?>(null) }
    val cancel = {
        if (state.editorDirty) {
            dialog = ProfileDialog.Fixed.Discard
        } else {
            onAction(ProfileAction.CloseEditor)
        }
    }
    BackHandler(state.editor != null || state.busy || state.token != null) {
        when {
            state.busy -> Unit
            state.token != null -> onAction(ProfileAction.CloseToken)
            else -> cancel()
        }
    }

    val refreshing = state.loading || state.keysLoading
    val tracker = rememberPullRefreshTracker(refreshing)
    val pullRefreshing = tracker.requested && refreshing
    val canRefresh = !refreshing && !tracker.requested && !state.busy && state.editor == null && !loggingOut
    val refresh = {
        if (canRefresh) tracker.request { onAction(ProfileAction.Refresh) }
    }
    ProfileScreenContent(
        state = state,
        username = username,
        theme = theme,
        appearanceError = appearanceError,
        loggingOut = loggingOut,
        timeZone = timeZone,
        pullRefreshing = pullRefreshing,
        canRefresh = canRefresh,
        onTheme = onTheme,
        onAction = onAction,
        onRefresh = refresh,
        cancel = cancel,
        onShowDialog = { dialog = it },
        dashboardSettings = dashboardSettings,
        debugContent = debugContent,
    )
    ProfileDialogs(
        dialog = dialog,
        state = state,
        otherEdits = otherEdits,
        onDismiss = { dialog = null },
        onLogout = onLogout,
        onAction = onAction,
    )
}

@Composable
private fun ProfileScreenContent(
    state: ProfileUiState,
    username: String,
    theme: ThemePreference,
    appearanceError: UiText?,
    loggingOut: Boolean,
    timeZone: ZoneId,
    pullRefreshing: Boolean,
    canRefresh: Boolean,
    onTheme: (ThemePreference) -> Unit,
    onAction: (ProfileAction) -> Unit,
    onRefresh: () -> Unit,
    cancel: () -> Unit,
    onShowDialog: (ProfileDialog) -> Unit,
    dashboardSettings: @Composable () -> Unit = {},
    debugContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refreshLabel = stringResource(R.string.pulse_profile_refresh)
    PullToRefreshBox(
        isRefreshing = pullRefreshing,
        onRefresh = onRefresh,
        modifier =
            modifier.fillMaxSize().padding(LocalPulseContentPadding.current).semantics {
                customActions =
                    if (canRefresh) {
                        listOf(
                            CustomAccessibilityAction(refreshLabel) {
                                onRefresh()
                                true
                            },
                        )
                    } else {
                        emptyList()
                    }
            },
    ) {
        PulseAdaptiveLayout(Modifier.fillMaxSize().imePadding()) { size ->
            if (state.loading &&
                state.profile == null &&
                !state.keysLoaded &&
                state.editor == null &&
                state.token == null &&
                !state.uncertain &&
                !pullRefreshing
            ) {
                PulseCenteredLoadingState(stringResource(R.string.pulse_profile_profile_loading))
                return@PulseAdaptiveLayout
            }
            PulseFormLayout(
                modifier = Modifier.widthIn(max = PulseTokens.contentWidth),
                scrollContentModifier = Modifier.padding(size.pagePadding),
                footer = {
                    state.editor?.let { editor ->
                        PulseFormActions(
                            primary =
                                PulseFormAction(
                                    stringResource(
                                        if (editor == AccountEditor.Key) CoreUiR.string.pulse_ui_create else CoreUiR.string.pulse_ui_save,
                                    ),
                                    { onAction(ProfileAction.SaveEditor) },
                                    enabled = state.canMutate,
                                    loading = state.busy,
                                ),
                            secondary = PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), cancel, enabled = !state.busy),
                            modifier =
                                Modifier
                                    .align(Alignment.Start)
                                    .padding(horizontal = size.pagePadding)
                                    .widthIn(max = PulseTokens.readingWidth),
                        )
                    }
                },
            ) {
                ProfileReviewCard(state, onAction)
                if (state.editor != null) {
                    AccountForm(state, onAction)
                    if (state.editor != AccountEditor.Password) {
                        PulseButton(stringResource(R.string.pulse_profile_logout), {
                            onShowDialog(ProfileDialog.Fixed.Logout)
                        }, enabled = !state.busy && !loggingOut, style = PulseButtonStyle.Text)
                    }
                } else {
                    val identity: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                                PulseText(state.profile?.name ?: username, Modifier.semantics { heading() }, PulseTextStyle.Display)
                                PulseText(stringResource(R.string.pulse_profile_username, username))
                                if (state.loading && state.profile == null && !pullRefreshing) {
                                    PulseCenteredLoadingState(
                                        stringResource(R.string.pulse_profile_profile_loading),
                                        Modifier.fillMaxWidth(),
                                    )
                                }
                                state.profileError?.let { PulseErrorState(it.message, { onAction(ProfileAction.RefreshProfile) }) }
                                if (state.profile?.hasProfile == false) PulseText(stringResource(R.string.pulse_profile_profile_fallback))
                                PulseButton(
                                    stringResource(R.string.pulse_profile_edit_name),
                                    { onAction(ProfileAction.Edit(AccountEditor.Name)) },
                                    enabled = state.canMutate,
                                )
                            }
                            ProfilePreferenceCard(ProfilePreference.TimeZone, state) {
                                onShowDialog(ProfileDialog.Fixed.TimeZone)
                            }
                            ProfilePreferenceCard(ProfilePreference.Currency, state) {
                                onShowDialog(ProfileDialog.Fixed.Currency)
                            }
                            PulseCard(Modifier.fillMaxWidth()) {
                                PulseText(stringResource(R.string.pulse_profile_appearance), style = PulseTextStyle.Title)
                                Column(Modifier.selectableGroup()) {
                                    ThemePreference.entries.forEach { choice ->
                                        PulseRadioButton(
                                            theme == choice,
                                            { onTheme(choice) },
                                            choice.label,
                                            Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                appearanceError?.let { PulseText(it, color = PulseTheme.colors.error) }
                            }
                            dashboardSettings()
                        }
                    }
                    val account: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            PulseCard(Modifier.fillMaxWidth()) {
                                PulseText(
                                    stringResource(R.string.pulse_profile_account),
                                    Modifier.semantics { heading() },
                                    PulseTextStyle.Title,
                                )
                                PulseButton(
                                    stringResource(R.string.pulse_profile_password),
                                    { onAction(ProfileAction.Edit(AccountEditor.Password)) },
                                    enabled = state.canMutate,
                                    style = PulseButtonStyle.Text,
                                )
                            }
                            APIKeysCard(state, timeZone, pullRefreshing, onAction) { onShowDialog(ProfileDialog.Revoke(it)) }
                            PulseCard(Modifier.fillMaxWidth()) {
                                PulseText(stringResource(R.string.pulse_profile_about), style = PulseTextStyle.Title)
                                Image(
                                    painterResource(CoreUiR.drawable.pulse_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                )
                                PulseText(stringResource(R.string.pulse_profile_brand), style = PulseTextStyle.Display)
                                PulseText(stringResource(R.string.pulse_profile_tagline), style = PulseTextStyle.Title)
                            }
                            PulseButton(stringResource(R.string.pulse_profile_logout), {
                                onShowDialog(ProfileDialog.Fixed.Logout)
                            }, Modifier.fillMaxWidth(), enabled = !state.busy && !loggingOut, loading = loggingOut)
                            debugContent()
                        }
                    }
                    if (size == PulseWindowSize.Compact ||
                        LocalDensity.current.fontScale > 1.3f
                    ) {
                        identity()
                        account()
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                            Box(Modifier.weight(1f)) { identity() }
                            Box(Modifier.weight(1f)) { account() }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenContentPreview() {
    PulseTheme {
        ProfileScreenContent(
            state =
                ProfileUiState(
                    profileLoad = ProfileLoad.Loaded(Profile(name = "Asha")),
                    keysLoad = ProfileLoad.Loaded(emptyList()),
                ),
            username = "asha",
            theme = ThemePreference.System,
            appearanceError = null,
            loggingOut = false,
            timeZone = ZoneId.of("UTC"),
            pullRefreshing = false,
            canRefresh = true,
            onTheme = {},
            onAction = {},
            onRefresh = {},
            cancel = {},
            onShowDialog = {},
            debugContent = {},
        )
    }
}
