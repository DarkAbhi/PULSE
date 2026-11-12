package dev.abhishekan.pulse.feature.garage

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.abhishekan.pulse.core.ui.LocalPulseContentPadding
import dev.abhishekan.pulse.core.ui.PulseAdaptiveLayout
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseDialog
import dev.abhishekan.pulse.core.ui.components.PulseErrorState
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseRefreshIndicator
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.core.ui.pagePadding

@Composable
internal fun GarageEditorLayout(
    editor: GarageEditorState,
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
    content: @Composable (enabled: Boolean) -> Unit,
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
    BackHandler(draft != null || editor.busy) {
        if (!editor.busy) cancel()
    }

    val editorTitle =
        draft?.let {
            val action = if (it.id == null && it.kind != GarageForm.Pressure) EditorAction.Add else EditorAction.Edit
            stringResource(it.kind.editorLabel(action))
        }
    val scrollStates = rememberSaveableStateHolder()
    PulseScreenScaffold(
        title = editorTitle ?: title,
        showTopBar = draft != null || backLabel != null,
        backLabel = if (draft != null) stringResource(R.string.pulse_garage_back_to_garage) else backLabel,
        backEnabled = !editor.busy,
    ) {
        PulseAdaptiveLayout(Modifier.fillMaxSize()) { size ->
            val body: @Composable () -> Unit = {
                val fuelEditor = draft?.kind == GarageForm.Fuel && !showingRecords
                val editorScroll = rememberScrollState()
                LaunchedEffect(draft?.kind, draft?.vehicleStep) {
                    if (draft?.kind == GarageForm.Vehicle) editorScroll.scrollTo(0)
                }
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
                        if (draft != null && !showingRecords) {
                            GarageEditor(
                                draft,
                                editor.busy,
                                editor.reviewRequired,
                                editor.error,
                                onUpdate,
                                onSave,
                                cancel,
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
                            content(!editor.busy && !editor.reviewRequired && draft == null)
                        }
                        if (editor.busy && showBusyIndicator) {
                            PulseLoadingState(stringResource(R.string.pulse_garage_working_on_your_garage))
                        }
                    }
                    if (fuelEditor) {
                        Box(Modifier.padding(horizontal = size.pagePadding)) {
                            FuelEditorActions(editor.busy, editor.reviewRequired, onSave, cancel)
                        }
                    }
                }
            }

            Column(Modifier.widthIn(max = PulseTokens.contentWidth).fillMaxSize()) {
                header(showingRecords)
                Box(Modifier.weight(1f).fillMaxWidth().then(bodyModifier)) {
                    when {
                        !editor.reviewRequired && showingRecords && loading ->
                            PulseLoadingState(
                                stringResource(R.string.pulse_garage_loading_your_garage),
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .padding(LocalPulseContentPadding.current)
                                        .padding(size.pagePadding),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            )
                        draft != null && !showingRecords -> key(draft.kind, draft.id) { body() }
                        else -> scrollStates.SaveableStateProvider(scrollKey) { body() }
                    }
                    overlay()
                }
            }
        }
    }

    if (discard) {
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
    showRefreshIndicator: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    if (showRefreshIndicator && section.loading && section.value != null) PulseRefreshIndicator()
    section.error?.let { PulseErrorState(it.message, retry) }
    section.value?.let { content(it) }
}
