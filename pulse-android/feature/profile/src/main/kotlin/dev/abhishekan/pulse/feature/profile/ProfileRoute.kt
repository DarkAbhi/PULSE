package dev.abhishekan.pulse.feature.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseHideNavigation
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSnackbarEffect
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileSetupPrompt(
    onSetup: () -> Unit,
    onSkip: () -> Unit,
) {
    PulseDialog(
        stringResource(R.string.pulse_profile_onboard_title),
        stringResource(R.string.pulse_profile_onboard_body),
        stringResource(R.string.pulse_profile_set_name),
        onSetup,
        onSkip,
        dismissLabel = stringResource(R.string.pulse_profile_not_now),
    )
}

@Composable
fun ProfileRoute(
    username: String,
    theme: ThemePreference,
    appearanceError: UiText?,
    onTheme: (ThemePreference) -> Unit,
    loggingOut: Boolean,
    otherEdits: Boolean,
    onLogout: () -> Unit,
    initialNameEditor: Boolean,
    viewModel: ProfileViewModel = koinViewModel(),
    backLabel: String? = null,
    debugContent: @Composable () -> Unit = {},
) {
    val timeZone by UserTimeZone.changes.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PulseHideNavigation(state.editor != null || state.token != null)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val messages =
        remember(viewModel, lifecycle) {
            viewModel.events.flowWithLifecycle(lifecycle).map { it.message }
        }
    PulseSnackbarEffect(messages)
    val context = LocalContext.current
    val activity = context as? Activity
    DisposableEffect(viewModel, activity) {
        onDispose {
            if (activity?.isChangingConfigurations != true) {
                viewModel.onAction(ProfileAction.ClearSecrets)
            }
        }
    }
    val sensitive = state.token != null || state.editor == AccountEditor.Password
    DisposableEffect(activity, sensitive) {
        val window = activity?.window
        val flag = WindowManager.LayoutParams.FLAG_SECURE
        val addedSecureFlag = sensitive && window != null && (window.attributes.flags and flag) == 0
        if (addedSecureFlag) window.addFlags(flag)
        onDispose { if (addedSecureFlag) window.clearFlags(flag) }
    }
    LaunchedEffect(viewModel, initialNameEditor) {
        if (initialNameEditor) {
            viewModel.onAction(ProfileAction.OpenInitialNameEditor)
        }
    }
    PulseScreenScaffold(
        title =
            stringResource(
                when (state.editor) {
                    AccountEditor.Name -> R.string.pulse_profile_edit_name
                    AccountEditor.Password -> R.string.pulse_profile_password
                    AccountEditor.Key -> R.string.pulse_profile_create_key
                    null -> R.string.pulse_profile_screen_title
                },
            ),
        showTopBar = state.editor != null || backLabel != null,
        backLabel = if (state.editor != null) stringResource(R.string.pulse_profile_back_to_you) else backLabel,
        backEnabled = !state.busy,
    ) {
        ProfileScreen(
            state = state,
            username = username,
            theme = theme,
            appearanceError = appearanceError,
            loggingOut = loggingOut,
            otherEdits = otherEdits,
            timeZone = timeZone,
            onTheme = onTheme,
            onLogout = onLogout,
            onAction = viewModel::onAction,
            debugContent = debugContent,
        )
    }
    state.token?.let { created ->
        PulseDialog(
            stringResource(R.string.pulse_profile_token_title),
            stringResource(R.string.pulse_profile_token_content, created.token, state.tokenCopyMessage?.asString().orEmpty()),
            stringResource(R.string.pulse_profile_copy),
            { viewModel.onAction(ProfileAction.TokenCopied(copySecret(context, created.token))) },
            { viewModel.onAction(ProfileAction.CloseToken) },
            dismissLabel = stringResource(R.string.pulse_profile_close),
        )
    }
}

// The string constant is inlined; older systems ignore this preview hint.
@SuppressLint("InlinedApi")
internal fun copySecret(
    context: Context,
    token: String,
): Boolean =
    try {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText(context.getString(R.string.pulse_profile_clipboard_label), token)
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        @Suppress("UsePropertyAccessSyntax")
        clipboard.setPrimaryClip(clip)
        clipboard.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString() == token
    } catch (_: Exception) {
        false
    }
