package dev.abhishekan.pulse.feature.horizon.presentation.statementimport

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.domain.model.StatementBatch
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.domain.model.StatementRows
import dev.abhishekan.pulse.core.domain.model.UserTimeZone
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseDatePickerDialog
import dev.abhishekan.pulse.core.ui.components.PulseFormAction
import dev.abhishekan.pulse.core.ui.components.PulseFormActions
import dev.abhishekan.pulse.core.ui.components.PulseFormLayout
import dev.abhishekan.pulse.core.ui.components.PulseHorizontalDivider
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import dev.abhishekan.pulse.feature.horizon.R
import dev.abhishekan.pulse.feature.horizon.presentation.components.horizonOption
import dev.abhishekan.pulse.feature.horizon.presentation.editor.HorizonFormSection
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionInput
import dev.abhishekan.pulse.feature.horizon.presentation.editor.SubscriptionSegments
import dev.abhishekan.pulse.feature.horizon.presentation.editor.TransactionPickerField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MAX_PDF_BYTES = 10 * 1024 * 1024

@Composable
internal fun StatementImportScreen(
    categories: List<HorizonCategory>,
    onClose: () -> Unit,
    onManageAccounts: () -> Unit,
    onCompleted: () -> Unit,
    vm: StatementImportViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.applicationContext.contentResolver
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.onAction(StatementImportAction.Upload { readStatement(resolver, uri) })
        }
    LaunchedEffect(vm) {
        vm.onAction(StatementImportAction.Entered)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val close by rememberUpdatedState(onClose)
    val manageAccounts by rememberUpdatedState(onManageAccounts)
    val completed by rememberUpdatedState(onCompleted)
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.uiState
                .map { it.exit as? StatementImportExit.Pending }
                .distinctUntilChanged()
                .collectLatest { pending ->
                    val event = pending?.event ?: return@collectLatest
                    vm.onAction(StatementImportAction.ExitHandled(event))
                    when (event) {
                        StatementImportEvent.Closed -> close()
                        StatementImportEvent.ManageAccounts -> manageAccounts()
                        is StatementImportEvent.Completed -> completed()
                    }
                }
        }
    }
    BackHandler { vm.onAction(StatementImportAction.Close) }

    val listState = key(state.batch?.id, state.page?.page) { rememberLazyListState() }
    LaunchedEffect(listState, state.errorCode, state.failure) {
        if (state.batch != null && state.page != null && (state.errorCode != null || state.failure != null)) {
            listState.scrollToItem(0)
        }
    }
    StatementImportScreenContent(state, categories, listState, vm::onAction) {
        picker.launch(arrayOf("application/pdf"))
    }
}

@Composable
private fun StatementImportScreenContent(
    state: StatementImportState,
    categories: List<HorizonCategory>,
    listState: LazyListState,
    onAction: (StatementImportAction) -> Unit,
    onPickPdf: () -> Unit,
) {
    PulseScreenScaffold(
        title = stringResource(R.string.pulse_horizon_import_statement),
        backLabel = stringResource(R.string.pulse_horizon_back_to_horizon),
        backEnabled = !state.busy,
        onBack = { onAction(StatementImportAction.Close) },
    ) {
        val batch = state.batch
        val page = state.page
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            when {
                state.exit != null -> Unit
                batch != null && page != null ->
                    key(batch.id, page.page) {
                        StatementReview(state, batch, page, categories, listState, onAction)
                    }
                else ->
                    StatementSetup(state, onAction, onPickPdf)
            }
            if (state.busy && state.exit == null) {
                val progress = state.progress
                val label =
                    when {
                        batch != null && page != null -> stringResource(R.string.pulse_horizon_statement_loading)
                        state.accounts == null -> stringResource(R.string.pulse_horizon_statement_loading_accounts)
                        !state.uploading || progress == null -> stringResource(R.string.pulse_horizon_statement_uploading)
                        progress < 1 -> stringResource(R.string.pulse_horizon_statement_percent, (progress * 100).toInt())
                        else -> stringResource(R.string.pulse_horizon_statement_extracting)
                    }
                PulseCard(Modifier.align(Alignment.Center).padding(horizontal = PulseTokens.medium)) {
                    PulseLoadingState(label, horizontalAlignment = Alignment.CenterHorizontally)
                }
            }
        }
    }
}

@Composable
private fun StatementSetup(
    state: StatementImportState,
    onAction: (StatementImportAction) -> Unit,
    onPickPdf: () -> Unit,
) {
    val accounts = state.accounts
    PulseFormLayout(
        modifier = Modifier.widthIn(max = PulseTokens.readingWidth).padding(horizontal = PulseTokens.medium),
        scrollContentModifier = Modifier.padding(vertical = PulseTokens.medium),
        footer = {
            val primary =
                when {
                    accounts?.isNotEmpty() == true ->
                        PulseFormAction(
                            stringResource(R.string.pulse_horizon_statement_pick),
                            onPickPdf,
                            enabled = !state.busy && state.account != null,
                            modifier = Modifier.testTag("statement-pick"),
                        )
                    accounts?.isEmpty() == true ->
                        PulseFormAction(
                            stringResource(R.string.pulse_horizon_add_account),
                            { onAction(StatementImportAction.ManageAccounts) },
                        )
                    state.failure != null ->
                        PulseFormAction(
                            stringResource(R.string.pulse_horizon_retry_accounts),
                            { onAction(StatementImportAction.RetryAccounts) },
                            enabled = !state.busy,
                        )
                    else -> null
                }
            if (primary != null) PulseFormActions(primary)
        },
    ) {
        StatementImportError(state)
        if (accounts?.isEmpty() == true) {
            HorizonFormSection(
                R.string.pulse_horizon_statement_no_accounts,
                R.string.pulse_horizon_accounts_empty,
                R.drawable.pulse_obligation_wallet,
            ) {}
        }
        if (accounts?.isNotEmpty() == true) {
            HorizonFormSection(
                R.string.pulse_horizon_statement_source,
                R.string.pulse_horizon_statement_source_hint,
                R.drawable.pulse_obligation_document,
            ) {
                PulseSelectionField(
                    label = stringResource(R.string.pulse_horizon_statement_account),
                    selected =
                        state.account?.let { "${it.displayName} · ${it.institutionName}" }
                            ?: stringResource(R.string.pulse_horizon_statement_choose_account),
                    options = accounts.map { "${it.displayName} · ${it.institutionName}" },
                    onSelect = { onAction(StatementImportAction.SelectAccount(accounts[it])) },
                    enabled = !state.busy,
                    labelAbove = true,
                )
            }
        }
    }
}

@Composable
private fun StatementReview(
    state: StatementImportState,
    batch: StatementBatch,
    page: StatementRows,
    categories: List<HorizonCategory>,
    listState: LazyListState,
    onAction: (StatementImportAction) -> Unit,
) {
    val focus = LocalFocusManager.current

    Column(
        Modifier
            .widthIn(max = PulseTokens.readingWidth)
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = PulseTokens.medium),
    ) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(vertical = PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            item(key = "review") {
                Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                    StatementImportError(state) { onAction(StatementImportAction.Reload) }
                    HorizonFormSection(
                        R.string.pulse_horizon_statement_review,
                        null,
                        R.drawable.pulse_obligation_card,
                    ) {
                        PulseText(
                            stringResource(R.string.pulse_horizon_statement_reconciled, batch.parsed, batch.duplicates),
                            style = PulseTextStyle.Label,
                        )
                        when {
                            batch.parsed == batch.duplicates -> StatementSupportingText(R.string.pulse_horizon_statement_duplicates_only)
                            batch.ocrUsed -> StatementSupportingText(R.string.pulse_horizon_statement_ocr)
                            else -> StatementSupportingText(R.string.pulse_horizon_statement_review_hint)
                        }
                        if (batch.accountConfirmationRequired) {
                            PulseCheckbox(
                                state.confirmed,
                                { onAction(StatementImportAction.ConfirmAccount(it)) },
                                stringResource(R.string.pulse_horizon_statement_account_confirm, state.account?.displayName.orEmpty()),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.busy,
                            )
                        }
                    }
                }
            }
            items(page.rows, key = { it.id }) { source ->
                ImportRowCard(state.edits[source.id] ?: source, batch.currency, categories, !state.busy) {
                    onAction(StatementImportAction.EditRow(it))
                }
            }
            if (page.total > page.pageSize) {
                item(key = "pagination") {
                    StatementPagination(page, !state.busy) {
                        focus.clearFocus()
                        onAction(StatementImportAction.Page(it))
                    }
                }
            }
        }
        PulseFormActions(
            primary =
                PulseFormAction(
                    stringResource(R.string.pulse_horizon_statement_commit),
                    {
                        focus.clearFocus()
                        onAction(StatementImportAction.Commit)
                    },
                    enabled = !state.busy && state.confirmed && state.errorCode !in setOf("BATCH_EXPIRED", "STALE_REVISION"),
                    modifier = Modifier.testTag("statement-confirm"),
                ),
            secondary =
                PulseFormAction(
                    stringResource(R.string.pulse_horizon_statement_cancel),
                    { onAction(StatementImportAction.Close) },
                    enabled = !state.busy,
                ),
        )
    }
}

@Composable
private fun StatementPagination(
    page: StatementRows,
    enabled: Boolean,
    onPage: (Int) -> Unit,
) {
    val pageCount = (page.total - 1) / page.pageSize + 1
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
    ) {
        PulseText(
            stringResource(R.string.pulse_horizon_statement_page, page.page, pageCount),
            style = PulseTextStyle.Label,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            PulseButton(
                stringResource(R.string.pulse_horizon_statement_previous),
                { onPage(page.page - 1) },
                Modifier.weight(1f),
                enabled = enabled && page.page > 1,
                variant = PulseButtonVariant.Secondary,
            )
            PulseButton(
                stringResource(R.string.pulse_horizon_statement_next),
                { onPage(page.page + 1) },
                Modifier.weight(1f),
                enabled = enabled && page.page < pageCount,
                variant = PulseButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun StatementImportError(
    state: StatementImportState,
    onReload: (() -> Unit)? = null,
) {
    val errorCode = state.errorCode
    val message = if (errorCode != null) stringResource(importErrorLabel(errorCode)) else state.failure?.message?.asString()
    if (message == null) return

    val colors = PulseTheme.colors
    PulseCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, colors.border), contentPadding = PulseTokens.medium) {
        PulseText(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = colors.error)
        if (errorCode == "STALE_REVISION" && onReload != null) {
            PulseButton(
                stringResource(R.string.pulse_horizon_statement_reload),
                onReload,
                enabled = !state.busy,
                variant = PulseButtonVariant.Secondary,
            )
        }
    }
}

@Composable
private fun StatementSupportingText(label: Int) {
    PulseText(stringResource(label), style = PulseTextStyle.Body2, color = PulseTheme.colors.textSecondary)
}

@Composable
private fun ImportRowCard(
    row: StatementRow,
    currency: String,
    categories: List<HorizonCategory>,
    enabled: Boolean,
    change: (StatementRow) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val colors = PulseTheme.colors
    val date = runCatching { LocalDate.parse(row.date) }.getOrNull()
    val dateLabel = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: row.date
    PulseCard(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, colors.border), contentPadding = PulseTokens.medium) {
        PulseText(
            row.description.ifBlank { stringResource(R.string.pulse_horizon_statement_no_description) },
            style = PulseTextStyle.Title3,
        )
        PulseText(
            stringResource(R.string.pulse_horizon_statement_row_summary, dateLabel, horizonOption(row.type), row.amount, currency),
            style = PulseTextStyle.Body2,
            color = colors.textSecondary,
        )
        if (row.duplicate) {
            StatementSupportingText(R.string.pulse_horizon_statement_duplicate)
        } else {
            if ("POSSIBLE_MANUAL_MATCH" in row.warnings) StatementSupportingText(R.string.pulse_horizon_statement_manual_match)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            ) {
                PulseCheckbox(
                    row.included,
                    { change(row.copy(included = it)) },
                    stringResource(R.string.pulse_horizon_statement_include),
                    Modifier.weight(1f),
                    enabled = enabled,
                )
                val detailsLabel =
                    if (expanded) R.string.pulse_horizon_statement_hide_details else R.string.pulse_horizon_statement_edit_details
                PulseButton(
                    stringResource(detailsLabel),
                    {
                        focus.clearFocus()
                        expanded = !expanded
                    },
                    modifier = Modifier.weight(0.8f),
                    enabled = enabled,
                    style = PulseButtonStyle.Text,
                )
            }
            if (expanded) {
                PulseHorizontalDivider()
                StatementRowFields(row, date, dateLabel, currency, categories, enabled, change)
            }
        }
    }
}

@Composable
private fun StatementRowFields(
    row: StatementRow,
    date: LocalDate?,
    dateLabel: String,
    currency: String,
    categories: List<HorizonCategory>,
    enabled: Boolean,
    change: (StatementRow) -> Unit,
) {
    var datePicker by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        SubscriptionInput(
            row.description,
            { if (it.length <= 255) change(row.copy(description = it)) },
            stringResource(R.string.pulse_horizon_statement_description),
            stringResource(R.string.pulse_horizon_transaction_name_hint),
            enabled,
        )
        SubscriptionInput(
            row.amount,
            { change(row.copy(amount = it)) },
            stringResource(R.string.pulse_horizon_statement_amount, currency),
            "0.00",
            enabled,
            KeyboardType.Decimal,
        )
        SubscriptionSegments(
            stringResource(R.string.pulse_horizon_statement_direction),
            row.type,
            listOf("debit", "credit"),
            enabled,
        ) { change(row.copy(type = it)) }
        TransactionPickerField(
            R.string.pulse_horizon_transaction_date,
            dateLabel.ifBlank { stringResource(R.string.pulse_horizon_not_set) },
            enabled,
        ) {
            focus.clearFocus()
            datePicker = true
        }
        val other = stringResource(R.string.pulse_horizon_account_other)
        PulseSelectionField(
            label = stringResource(R.string.pulse_horizon_statement_category),
            selected = categories.firstOrNull { it.id == row.categoryId }?.name ?: other,
            options = listOf(other) + categories.map(HorizonCategory::name),
            onSelect = { change(row.copy(categoryId = categories.getOrNull(it - 1)?.id)) },
            enabled = enabled,
            labelAbove = true,
        )
    }
    if (datePicker) {
        PulseDatePickerDialog(
            date ?: LocalDate.now(UserTimeZone.zone),
            {
                change(row.copy(date = it.toString()))
                datePicker = false
            },
            { datePicker = false },
        )
    }
}

private suspend fun readStatement(
    resolver: ContentResolver,
    uri: Uri,
): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            resolver.openInputStream(uri)?.use { input ->
                val result = ByteArrayOutputStream()
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    if (result.size() + size > MAX_PDF_BYTES) return@withContext null
                    result.write(buffer, 0, size)
                }
                result.toByteArray()
            }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

private fun importErrorLabel(code: String): Int =
    when (code) {
        "INVALID_FILE", "NOT_PDF", "FILE_TOO_LARGE" -> R.string.pulse_horizon_statement_bad_pdf
        "ENCRYPTED_PDF_UNSUPPORTED" -> R.string.pulse_horizon_statement_encrypted
        "UNSUPPORTED_BANK" -> R.string.pulse_horizon_statement_unsupported
        "CURRENCY_MISMATCH" -> R.string.pulse_horizon_statement_currency
        "ACCOUNT_MISMATCH" -> R.string.pulse_horizon_statement_wrong_account
        "ACCOUNT_ARCHIVED" -> R.string.pulse_horizon_statement_archived_account
        "RECONCILIATION_FAILED", "BALANCES_UNAVAILABLE" -> R.string.pulse_horizon_statement_bad_balance
        "ALREADY_IMPORTED" -> R.string.pulse_horizon_statement_already_imported
        "BATCH_EXPIRED" -> R.string.pulse_horizon_statement_expired
        "STALE_REVISION" -> R.string.pulse_horizon_statement_stale
        "INVALID_ROW" -> R.string.pulse_horizon_statement_invalid_row
        "EXTRACTOR_BUSY" -> R.string.pulse_horizon_statement_busy
        "EXTRACTION_TIMEOUT" -> R.string.pulse_horizon_statement_timeout
        "AMBIGUOUS_DUPLICATE" -> R.string.pulse_horizon_statement_ambiguous
        else -> R.string.pulse_horizon_statement_error
    }
