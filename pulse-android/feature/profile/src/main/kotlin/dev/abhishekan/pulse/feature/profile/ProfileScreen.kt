package dev.abhishekan.pulse.feature.profile

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.view.WindowManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.APIKey
import dev.abhishekan.pulse.core.domain.model.ThemePreference
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.PulseWindowSize
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseEmptyState
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulsePasswordField
import dev.abhishekan.pulse.core.ui.components.PulseRadioButton
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextButton
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.label
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.uiText
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    onEditorOpened: () -> Unit,
    viewModel: ProfileViewModel,
    onSensitiveEditsChanged: (Boolean) -> Unit = {},
    debugContent: @Composable () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val hasPasswordEdits =
        state.editor == AccountEditor.Password &&
            listOf(state.passwords.current, state.passwords.next, state.passwords.confirmation).any(String::isNotEmpty)
    SideEffect { onSensitiveEditsChanged(hasPasswordEdits) }
    DisposableEffect(viewModel, activity) {
        onDispose {
            if (activity?.isChangingConfigurations !=
                true
            ) {
                viewModel.clearSecrets()
                onSensitiveEditsChanged(false)
            }
        }
    }
    val sensitive = state.token != null || state.editor == AccountEditor.Password
    DisposableEffect(activity, sensitive) {
        val window = activity?.window
        if (sensitive) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(initialNameEditor) {
        if (initialNameEditor) {
            viewModel.edit(AccountEditor.Name)
            onEditorOpened()
        }
    }
    ProfileScreen(state, username, theme, appearanceError, onTheme, loggingOut, otherEdits, onLogout, viewModel, debugContent)
    state.token?.let { created ->
        PulseDialog(
            stringResource(R.string.pulse_profile_token_title),
            stringResource(R.string.pulse_profile_token_content, created.token, state.message?.asString().orEmpty()),
            stringResource(R.string.pulse_profile_copy),
            { viewModel.copied(copySecret(context, created.token)) },
            viewModel::clearToken,
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
        clipboard.setPrimaryClip(clip)
        clipboard.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString() == token
    } catch (_: Exception) {
        false
    }

@Composable
internal fun ProfileScreen(
    state: ProfileUiState,
    username: String,
    theme: ThemePreference,
    appearanceError: UiText?,
    onTheme: (ThemePreference) -> Unit,
    loggingOut: Boolean,
    otherEdits: Boolean,
    onLogout: () -> Unit,
    vm: ProfileViewModel,
    debugContent: @Composable () -> Unit = {},
) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var logout by rememberSaveable { mutableStateOf(false) }
    var revoke by remember { mutableStateOf<APIKey?>(null) }
    val dirty =
        when (state.editor) {
            AccountEditor.Name -> state.name != state.profile?.name.orEmpty()
            AccountEditor.Password ->
                listOf(
                    state.passwords.current,
                    state.passwords.next,
                    state.passwords.confirmation,
                ).any(String::isNotEmpty)
            AccountEditor.Key -> state.keyName.isNotBlank()
            null -> false
        }
    val cancel = { if (dirty) discard = true else vm.closeEditor() }
    BackHandler(state.editor != null || state.busy || state.token != null) {
        when {
            state.busy -> Unit
            state.token != null -> vm.clearToken()
            else -> cancel()
        }
    }
    PulseAdaptiveLayout(Modifier.fillMaxSize().imePadding()) { size ->
        Column(
            Modifier
                .widthIn(max = PulseTokens.contentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    if (size ==
                        PulseWindowSize.Compact
                    ) {
                        PulseTokens.medium
                    } else {
                        PulseTokens.large
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
        ) {
            if (state.uncertain) {
                PulseCard(Modifier.fillMaxWidth()) {
                    PulseText(
                        state.message ?: uiText(R.string.pulse_profile_uncertain),
                        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (!state.passwordUncertain) {
                        PulseText(
                            stringResource(
                                R.string.pulse_profile_server_name,
                                state.profile?.name ?: stringResource(R.string.pulse_profile_not_set),
                            ),
                        )
                        state.profileError?.let { PulseErrorState(it.message, vm::refreshProfile) }
                        if (state.keysLoaded) {
                            state.keys.forEach { key ->
                                PulseText(
                                    stringResource(R.string.pulse_profile_server_key, key.name, key.prefix),
                                    style = PulseTextStyle.Label,
                                )
                            }
                        }
                        state.keysError?.let { PulseErrorState(it.message, vm::refreshKeys) }
                        PulseTextButton(stringResource(R.string.pulse_profile_review), {
                            vm.refreshProfile()
                            vm.refreshKeys()
                        }, enabled = !state.busy)
                    }
                    PulseButton(
                        stringResource(if (state.passwordUncertain) R.string.pulse_profile_understood else R.string.pulse_profile_reviewed),
                        vm::acknowledge,
                        enabled =
                            state.reviewed && !state.busy,
                    )
                }
            } else {
                state.message?.let { PulseText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            }
            if (state.editor != null) {
                AccountForm(state, vm, cancel)
                PulseTextButton(stringResource(R.string.pulse_profile_logout), { logout = true }, enabled = !state.busy && !loggingOut)
            } else {
                val identity: @Composable () -> Unit = {
                    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding)) {
                        PulseCard(Modifier.fillMaxWidth(), highlighted = true) {
                            PulseText(state.profile?.name ?: username, Modifier.semantics { heading() }, PulseTextStyle.Display)
                            PulseText(stringResource(R.string.pulse_profile_identity))
                            PulseText(stringResource(R.string.pulse_profile_username, username))
                            if (state.loading) PulseLoadingState(stringResource(R.string.pulse_profile_profile_loading))
                            state.profileError?.let { PulseErrorState(it.message, vm::refreshProfile) }
                            if (state.profile?.hasProfile == false) PulseText(stringResource(R.string.pulse_profile_profile_fallback))
                            PulseButton(
                                stringResource(R.string.pulse_profile_edit_name),
                                { vm.edit(AccountEditor.Name) },
                                enabled =
                                    !state.busy && !state.uncertain,
                            )
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
                            PulseTextButton(
                                stringResource(R.string.pulse_profile_password),
                                { vm.edit(AccountEditor.Password) },
                                enabled =
                                    !state.busy && !state.uncertain,
                            )
                            PulseTextButton(
                                stringResource(R.string.pulse_profile_refresh_profile),
                                vm::refreshProfile,
                                enabled = !state.loading,
                            )
                        }
                        APIKeysCard(state, vm) { revoke = it }
                        PulseCard(Modifier.fillMaxWidth()) {
                            PulseText(stringResource(R.string.pulse_profile_about), style = PulseTextStyle.Title)
                            Image(
                                painterResource(dev.abhishekan.pulse.core.ui.R.drawable.pulse_logo),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                            )
                            PulseText(stringResource(R.string.pulse_profile_brand), style = PulseTextStyle.Display)
                            PulseText(stringResource(R.string.pulse_profile_tagline), style = PulseTextStyle.Title)
                            PulseText(stringResource(R.string.pulse_profile_description))
                        }
                        PulseButton(stringResource(R.string.pulse_profile_logout), {
                            logout = true
                        }, Modifier.fillMaxWidth(), enabled = !state.busy, loading = loggingOut)
                        debugContent()
                    }
                }
                if (size == PulseWindowSize.Compact ||
                    androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
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
    if (discard) {
        PulseDialog(
            stringResource(
                R.string.pulse_profile_discard_title,
            ),
            stringResource(R.string.pulse_profile_discard_body),
            stringResource(R.string.pulse_profile_discard),
            {
                discard =
                    false
                vm.closeEditor()
            },
            { discard = false },
        )
    }
    if (logout) {
        PulseDialog(
            stringResource(
                if (otherEdits ||
                    dirty
                ) {
                    dev.abhishekan.pulse.core.ui.R.string.pulse_ui_logout_edits_title
                } else {
                    R.string.pulse_profile_logout_title
                },
            ),
            stringResource(
                if (otherEdits ||
                    dirty
                ) {
                    R.string.pulse_profile_logout_edits
                } else {
                    R.string.pulse_profile_logout_body
                },
            ),
            stringResource(R.string.pulse_profile_logout),
            {
                logout = false
                vm.clearSecrets()
                onLogout()
            },
            { logout = false },
        )
    }
    revoke?.let { key ->
        PulseDialog(
            stringResource(
                R.string.pulse_profile_revoke_title,
            ),
            stringResource(R.string.pulse_profile_revoke_body, key.name),
            stringResource(R.string.pulse_profile_revoke, key.name),
            {
                revoke =
                    null
                vm.revoke(key.id)
            },
            { revoke = null },
        )
    }
}

@Composable
private fun AccountForm(
    state: ProfileUiState,
    vm: ProfileViewModel,
    cancel: () -> Unit,
) {
    val editor = checkNotNull(state.editor)
    val focus = LocalFocusManager.current
    PulseCard(Modifier.widthIn(max = PulseTokens.readingWidth).fillMaxWidth()) {
        PulseText(
            stringResource(
                when (editor) {
                    AccountEditor.Name -> R.string.pulse_profile_edit_name
                    AccountEditor.Password -> R.string.pulse_profile_password
                    AccountEditor.Key -> R.string.pulse_profile_create_key
                },
            ),
            style = PulseTextStyle.Title,
        )
        when (editor) {
            AccountEditor.Name ->
                PulseTextField(
                    state.name,
                    vm::name,
                    stringResource(R.string.pulse_profile_name),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.PersonFullName
                    },
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.save() }),
                )
            AccountEditor.Key ->
                PulseTextField(
                    state.keyName,
                    vm::keyName,
                    stringResource(R.string.pulse_profile_key_name),
                    Modifier.fillMaxWidth(),
                    enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { vm.save() }),
                )
            AccountEditor.Password -> {
                PulsePasswordField(
                    state.passwords.current,
                    {
                        vm.passwords(PasswordFields(it, state.passwords.next, state.passwords.confirmation))
                    },
                    stringResource(R.string.pulse_profile_current),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.Password
                    },
                    enabled = !state.busy,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(onNext = {
                            focus.moveFocus(FocusDirection.Down)
                        }),
                )
                PulsePasswordField(
                    state.passwords.next,
                    {
                        vm.passwords(PasswordFields(state.passwords.current, it, state.passwords.confirmation))
                    },
                    stringResource(R.string.pulse_profile_new),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.NewPassword
                    },
                    enabled = !state.busy,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                    keyboardActions =
                        KeyboardActions(onNext = {
                            focus.moveFocus(FocusDirection.Down)
                        }),
                )
                PulsePasswordField(
                    state.passwords.confirmation,
                    {
                        vm.passwords(PasswordFields(state.passwords.current, state.passwords.next, it))
                    },
                    stringResource(R.string.pulse_profile_confirmation),
                    Modifier.fillMaxWidth().semantics {
                        contentType =
                            ContentType.NewPassword
                    },
                    enabled = !state.busy,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            vm.save()
                        }),
                )
            }
        }
        state.error?.let {
            PulseText(
                it,
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = PulseTheme.colors.error,
            )
        }
        PulseButton(
            stringResource(R.string.pulse_profile_save),
            vm::save,
            Modifier.fillMaxWidth(),
            enabled =
                !state.busy && !state.uncertain,
            loading = state.busy,
        )
        PulseTextButton(stringResource(R.string.pulse_profile_cancel), cancel, enabled = !state.busy)
    }
}

@Composable
private fun APIKeysCard(
    state: ProfileUiState,
    vm: ProfileViewModel,
    revoke: (APIKey) -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(stringResource(R.string.pulse_profile_keys), Modifier.semantics { heading() }, PulseTextStyle.Title)
        PulseText(stringResource(R.string.pulse_profile_keys_description))
        PulseTextButton(stringResource(R.string.pulse_profile_refresh_keys), vm::refreshKeys, enabled = !state.keysLoading)
        if (state.keysLoading) PulseLoadingState(stringResource(R.string.pulse_profile_keys))
        state.keysError?.let { PulseErrorState(it.message, vm::refreshKeys) }
        if (state.keysLoaded &&
            state.keys.isEmpty()
        ) {
            PulseEmptyState(stringResource(R.string.pulse_profile_no_keys), stringResource(R.string.pulse_profile_no_keys_body))
        }
        state.keys.forEach { key ->
            PulseText(key.name, style = PulseTextStyle.Label)
            PulseText(
                stringResource(
                    R.string.pulse_profile_key_metadata,
                    key.prefix,
                    DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault()).format(key.createdAt),
                ),
            )
            PulseTextButton(
                stringResource(R.string.pulse_profile_revoke, key.name),
                { revoke(key) },
                enabled =
                    !state.busy && !state.uncertain,
            )
        }
        PulseButton(
            stringResource(R.string.pulse_profile_create_key),
            { vm.edit(AccountEditor.Key) },
            enabled =
                !state.busy && !state.uncertain,
        )
    }
}
