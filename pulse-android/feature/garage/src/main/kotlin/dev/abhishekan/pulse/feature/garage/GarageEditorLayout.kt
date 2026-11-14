package dev.abhishekan.pulse.feature.garage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCenteredLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding
import dev.abhishekan.pulse.core.ui.R as CoreUiR

@Composable
internal fun GarageEditorLayout(
    editor: GarageEditorState,
    active: Boolean,
    scrollStates: SaveableStateHolder,
    title: String,
    backLabel: String?,
    loading: Boolean,
    reviewReady: Boolean,
    scrollKey: String,
    onRefresh: () -> Unit,
    onReview: () -> Unit,
    onDiscard: () -> Unit,
    onUpdate: (GarageDraft) -> Unit,
    onSave: () -> Unit,
    onRetryCatalog: () -> Unit,
    showBusyIndicator: Boolean = true,
    bodyModifier: Modifier = Modifier,
    header: @Composable (showingRecords: Boolean) -> Unit = {},
    overlay: @Composable BoxScope.() -> Unit = {},
    recordsEditor: (@Composable (enabled: Boolean, prefix: @Composable () -> Unit) -> Unit)? = null,
    recordsPager: PagerState? = null,
    content: @Composable (enabled: Boolean, page: Int?) -> Unit,
) {
    var discard by rememberSaveable { mutableStateOf(false) }
    var records by rememberSaveable { mutableStateOf(false) }
    val draft = editor.draft
    val showingRecords = draft == null || records
    LaunchedEffect(draft == null) { if (draft == null) records = false }

    val cancel = {
        if (draft?.dirty == true) {
            discard = true
        } else {
            records = false
            onDiscard()
        }
    }
    val focus = LocalFocusManager.current
    val changeStep: (Int) -> Unit = { step ->
        focus.clearFocus()
        draft?.let { onUpdate(it.withVehicleStep(step)) }
    }
    val vehicleStep = draft?.takeIf { it.kind == GarageForm.Vehicle && !records }?.vehicleStep
    BackHandler(active && (draft != null || editor.busy)) {
        if (!editor.busy) {
            if (vehicleStep != null && vehicleStep > 0) changeStep(vehicleStep - 1) else cancel()
        }
    }

    val editorTitle =
        draft?.let {
            val action = if (it.id == null && it.kind != GarageForm.Pressure) EditorAction.Add else EditorAction.Edit
            stringResource(it.kind.editorLabel(action))
        }
    PulseScreenScaffold(
        title = editorTitle ?: title,
        showTopBar = draft != null || backLabel != null,
        backLabel = if (draft != null) stringResource(R.string.pulse_garage_back_to_garage) else backLabel,
        backEnabled = active && !editor.busy,
        onBack = if (vehicleStep != null) cancel else null,
    ) {
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
            val body: @Composable (Int?) -> Unit = { page ->
                val fuelEditor = draft?.kind == GarageForm.Fuel && !showingRecords
                val editorScroll = rememberScrollState()
                LaunchedEffect(draft?.kind, draft?.vehicleStep) {
                    if (draft?.kind == GarageForm.Vehicle) editorScroll.scrollTo(0)
                }
                val enabled = active && !editor.busy && !editor.reviewRequired && draft == null
                val review: @Composable () -> Unit = {
                    if (editor.reviewRequired) {
                        GarageReview(
                            editor,
                            reviewReady,
                            onRefresh,
                            onReview,
                            if (draft !=
                                null
                            ) {
                                ({ records = !records })
                            } else {
                                null
                            },
                            records,
                        )
                    }
                }
                if (recordsEditor != null && draft == null) {
                    recordsEditor(enabled, review)
                } else {
                    Column(
                        Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).imePadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(editorScroll)
                                .padding(size.pagePadding),
                            verticalArrangement = Arrangement.spacedBy(PulseTokens.cardPadding),
                            horizontalAlignment = if (fuelEditor) Alignment.CenterHorizontally else Alignment.Start,
                        ) {
                            review()
                            if (draft != null && !showingRecords) {
                                GarageEditor(
                                    draft,
                                    editor.busy,
                                    editor.error,
                                    onUpdate,
                                    makes = editor.makes,
                                    models = editor.models,
                                    retryCatalog = onRetryCatalog,
                                    match = editor.match,
                                )
                            } else {
                                if (draft != null) {
                                    PulseButton(
                                        stringResource(R.string.pulse_garage_resume_unsaved_edits),
                                        { records = false },
                                        style = PulseButtonStyle.Text,
                                    )
                                }
                                content(enabled && (page == null || page == recordsPager?.settledPage), page)
                            }
                            if (editor.busy && showBusyIndicator && showingRecords) {
                                PulseLoadingState(stringResource(R.string.pulse_garage_working_on_your_garage))
                            }
                        }
                        if (draft != null && !showingRecords) {
                            GarageFormFooter(
                                draft,
                                editor.busy,
                                editor.reviewRequired,
                                changeStep,
                                onSave,
                                cancel,
                                Modifier
                                    .align(if (fuelEditor) Alignment.CenterHorizontally else Alignment.Start)
                                    .padding(horizontal = size.pagePadding),
                            )
                        }
                    }
                }
            }

            Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize()) {
                header(showingRecords)
                Box(Modifier.weight(1f).fillMaxWidth().then(bodyModifier)) {
                    when {
                        !editor.reviewRequired && showingRecords && loading ->
                            PulseCenteredLoadingState(
                                stringResource(R.string.pulse_garage_loading_your_garage),
                                Modifier.fillMaxSize().padding(LocalPulseContentPadding.current).padding(size.pagePadding),
                            )
                        draft != null && !showingRecords -> key(draft.kind, draft.id) { body(null) }
                        recordsPager != null -> {
                            HorizontalPager(
                                state = recordsPager,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = !editor.busy,
                            ) { page ->
                                val semantics =
                                    if (page == recordsPager.settledPage) {
                                        Modifier
                                    } else {
                                        Modifier.clearAndSetSemantics { hideFromAccessibility() }
                                    }
                                Box(Modifier.fillMaxSize().then(semantics)) {
                                    scrollStates.SaveableStateProvider(page.toString()) { body(page) }
                                }
                            }
                        }
                        else -> scrollStates.SaveableStateProvider(if (draft == null) scrollKey else "review-$scrollKey") { body(null) }
                    }
                    overlay()
                }
            }
        }
    }

    if (active && discard) {
        PulseDialog(
            stringResource(R.string.pulse_garage_discard_unsaved_changes),
            stringResource(R.string.pulse_garage_your_edits_have_not_been_saved_discard_this_device),
            stringResource(R.string.pulse_garage_discard_edits),
            {
                discard = false
                records = false
                onDiscard()
            },
            { discard = false },
        )
    }
}

@Composable
internal fun GarageFormFooter(
    draft: GarageDraft,
    busy: Boolean,
    blocked: Boolean,
    changeStep: (Int) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val step = draft.vehicleStep.takeIf { draft.kind == GarageForm.Vehicle }
    val advancing = step != null && step < 2
    val canSubmit =
        when (step) {
            0 -> draft.vehicleIdentityValid
            1 -> draft.vehicleFuelValid
            2 -> !blocked && draft.vehicleIdentityValid && draft.vehicleFuelValid
            else -> !blocked
        }
    val cancelAction =
        PulseFormAction(stringResource(CoreUiR.string.pulse_ui_cancel), {
            focus.clearFocus()
            cancel()
        }, enabled = !busy)
    val back =
        step?.takeIf { it > 0 }?.let {
            PulseFormAction(stringResource(CoreUiR.string.pulse_ui_back), { changeStep(it - 1) }, enabled = !busy)
        }
    val primary =
        PulseFormAction(
            stringResource(if (advancing) CoreUiR.string.pulse_ui_next else CoreUiR.string.pulse_ui_save),
            {
                if (step != null && advancing) {
                    changeStep(step + 1)
                } else {
                    focus.clearFocus()
                    save()
                }
            },
            enabled = !busy && canSubmit,
            loading = busy && !advancing,
        )
    val footerWidth =
        when (draft.kind) {
            GarageForm.Fuel -> fuelFormWidth
            GarageForm.Vehicle -> 1200.dp
            GarageForm.Care -> 760.dp
            GarageForm.Pressure -> PulseTokens.readingWidth
        }
    BoxWithConstraints(modifier.widthIn(max = footerWidth).fillMaxWidth()) {
        if (step != null && maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f) {
            Row(horizontalArrangement = Arrangement.spacedBy(PulseTheme.spacing.large)) {
                Box(Modifier.weight(0.7f))
                PulseFormActions(primary, back ?: cancelAction, Modifier.weight(1.3f))
            }
        } else {
            PulseFormActions(primary, back ?: cancelAction)
        }
    }
}

@Composable
private fun GarageReview(
    editor: GarageEditorState,
    reviewReady: Boolean,
    onRefresh: () -> Unit,
    onReview: () -> Unit,
    onToggleRecords: (() -> Unit)?,
    records: Boolean,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        PulseText(
            stringResource(R.string.pulse_garage_review_before_another_change),
            style = PulseTextStyle.Title,
        )
        PulseText(
            editor.message?.asString()
                ?: stringResource(R.string.pulse_garage_a_previous_save_may_have_reached_the_server_review),
        )
        PulseButton(
            stringResource(R.string.pulse_garage_refresh_server_records),
            onRefresh,
            enabled = !editor.busy,
            style = PulseButtonStyle.Text,
        )
        if (onToggleRecords != null) {
            PulseButton(
                stringResource(
                    if (records) R.string.pulse_garage_return_to_draft else R.string.pulse_garage_view_server_records,
                ),
                onToggleRecords,
                style = PulseButtonStyle.Text,
            )
        }
        PulseButton(
            stringResource(R.string.pulse_garage_i_reviewed_the_server_records),
            onReview,
            enabled = reviewReady && !editor.busy,
        )
    }
}

@Composable
internal fun <T> GarageSection(
    section: GarageSection<T>,
    retry: () -> Unit,
    content: @Composable (T) -> Unit,
) {
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
