package dev.abhishekan.pulse.feature.horizon

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.abhishekan.pulse.core.domain.model.HorizonCategory
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.asString
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseCheckbox
import dev.abhishekan.pulse.core.ui.components.PulseLoadingState
import dev.abhishekan.pulse.core.ui.components.PulseScreenScaffold
import dev.abhishekan.pulse.core.ui.components.PulseSelectionField
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextField
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val MaxPdfBytes = 10 * 1024 * 1024

@Composable
internal fun StatementImportScreen(
    categories: List<HorizonCategory>,
    onClose: () -> Unit,
    onManageAccounts: () -> Unit,
    vm: StatementImportViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val resolver = LocalContext.current.contentResolver
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) vm.upload { readStatement(resolver, uri) }
        }
    LaunchedEffect(Unit) {
        if (vm.uiState.value.accounts == null && vm.uiState.value.failure == null) vm.loadAccounts()
    }
    val close = { vm.close(onClose) }
    BackHandler { close() }
    val title = stringResource(R.string.pulse_horizon_import_statement)
    PulseScreenScaffold(
        title,
        backLabel = stringResource(R.string.pulse_horizon_back_to_horizon),
        backEnabled = !state.busy,
        onBack = close,
    ) {
        val batch = state.batch
        val page = state.page
        val result = state.result
        LazyColumn(
            Modifier.fillMaxSize().imePadding().padding(horizontal = PulseTokens.medium),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(vertical = PulseTokens.medium),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.medium),
        ) {
            val errorCode = state.errorCode
            val failure = state.failure
            if (errorCode != null || failure != null) {
                item {
                    PulseText(if (errorCode != null) stringResource(importErrorLabel(errorCode)) else failure!!.message.asString())
                    if (errorCode == "STALE_REVISION") {
                        PulseButton(stringResource(R.string.pulse_horizon_statement_reload), vm::reload)
                    }
                    if (state.accounts == null) {
                        PulseButton(stringResource(R.string.pulse_horizon_retry_accounts), vm::loadAccounts)
                    }
                }
            }
            if (state.busy) {
                item {
                    val progress = state.progress
                    val label =
                        when {
                            !state.uploading -> stringResource(R.string.pulse_horizon_statement_loading)
                            progress == null -> stringResource(R.string.pulse_horizon_statement_uploading)
                            progress < 1 -> stringResource(R.string.pulse_horizon_statement_percent, (progress * 100).toInt())
                            else -> stringResource(R.string.pulse_horizon_statement_extracting)
                        }
                    PulseLoadingState(label)
                }
            }
            when {
                result != null ->
                    item {
                        PulseText(
                            stringResource(R.string.pulse_horizon_statement_result, result.imported, result.duplicates, result.excluded),
                        )
                        PulseButton(stringResource(R.string.pulse_horizon_statement_done), onClose)
                    }
                batch != null && page != null -> {
                    item {
                        PulseText(
                            stringResource(R.string.pulse_horizon_statement_reconciled, batch.parsed, batch.duplicates),
                            style = PulseTextStyle.Title,
                        )
                        if (batch.ocrUsed) PulseText(stringResource(R.string.pulse_horizon_statement_ocr))
                        if (batch.parsed == batch.duplicates) PulseText(stringResource(R.string.pulse_horizon_statement_duplicates_only))
                        if (batch.accountConfirmationRequired) {
                            PulseCheckbox(
                                state.confirmed,
                                vm::confirmAccount,
                                stringResource(R.string.pulse_horizon_statement_account_confirm, state.account?.displayName.orEmpty()),
                                enabled = !state.busy,
                            )
                        }
                    }
                    items(page.rows, key = { it.id }) { source ->
                        val row = state.edits[source.id] ?: source
                        ImportRowCard(row, batch.currency, categories, !state.busy && !source.duplicate, vm::edit)
                    }
                    item {
                        if (page.total > page.pageSize) {
                            Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                                PulseButton(
                                    stringResource(R.string.pulse_horizon_statement_previous),
                                    { vm.page(page.page - 1) },
                                    enabled =
                                        !state.busy && page.page > 1,
                                )
                                PulseButton(
                                    stringResource(R.string.pulse_horizon_statement_next),
                                    { vm.page(page.page + 1) },
                                    enabled =
                                        !state.busy && page.page * page.pageSize < page.total,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
                            PulseButton(
                                stringResource(R.string.pulse_horizon_statement_cancel),
                                close,
                                enabled = !state.busy,
                                style = PulseButtonStyle.Text,
                            )
                            PulseButton(
                                stringResource(R.string.pulse_horizon_statement_commit),
                                vm::commit,
                                enabled = !state.busy && state.confirmed && errorCode !in listOf("BATCH_EXPIRED", "STALE_REVISION"),
                                modifier = Modifier.testTag("statement-confirm"),
                            )
                        }
                    }
                }
                state.accounts?.isEmpty() == true ->
                    item {
                        PulseText(stringResource(R.string.pulse_horizon_accounts_empty))
                        PulseButton(stringResource(R.string.pulse_horizon_add_account), onManageAccounts)
                    }
                state.accounts?.isNotEmpty() == true ->
                    item {
                        val accounts = state.accounts.orEmpty()
                        PulseSelectionField(
                            label = stringResource(R.string.pulse_horizon_statement_account),
                            selected = state.account?.displayName ?: stringResource(R.string.pulse_horizon_statement_choose_account),
                            options = accounts.map { "${it.displayName} · ${it.institutionName}" },
                            onSelect = { vm.select(accounts[it]) },
                            enabled = !state.busy,
                        )
                        PulseButton(
                            stringResource(R.string.pulse_horizon_statement_pick),
                            { picker.launch(arrayOf("application/pdf")) },
                            enabled = !state.busy && state.account != null,
                            modifier = Modifier.testTag("statement-pick"),
                        )
                    }
            }
        }
    }
}

@Composable
private fun ImportRowCard(
    row: StatementRow,
    currency: String,
    categories: List<HorizonCategory>,
    enabled: Boolean,
    change: (StatementRow) -> Unit,
) {
    PulseCard(Modifier.fillMaxWidth()) {
        if (row.duplicate) {
            PulseText(row.description, style = PulseTextStyle.Title)
            PulseText("${row.date} · ${row.amount} $currency")
            PulseText(stringResource(R.string.pulse_horizon_statement_duplicate))
        } else {
            if ("POSSIBLE_MANUAL_MATCH" in row.warnings) PulseText(stringResource(R.string.pulse_horizon_statement_manual_match))
            PulseCheckbox(row.included, {
                change(row.copy(included = it))
            }, stringResource(R.string.pulse_horizon_statement_include), enabled = enabled)
            PulseTextField(
                row.description,
                {
                    if (it.length <=
                        255
                    ) {
                        change(row.copy(description = it))
                    }
                },
                label =
                    stringResource(
                        R.string.pulse_horizon_statement_description,
                    ),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            PulseTextField(row.date, {
                change(row.copy(date = it))
            }, label = stringResource(R.string.pulse_horizon_statement_date), enabled = enabled, modifier = Modifier.fillMaxWidth())
            PulseTextField(
                row.amount,
                {
                    change(row.copy(amount = it))
                },
                label =
                    stringResource(
                        R.string.pulse_horizon_statement_amount,
                        currency,
                    ),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
            val types = listOf("debit", "credit")
            val typeLabels =
                listOf(
                    stringResource(R.string.pulse_horizon_statement_debit),
                    stringResource(R.string.pulse_horizon_statement_credit),
                )
            PulseSelectionField(
                stringResource(R.string.pulse_horizon_statement_direction),
                typeLabels[types.indexOf(row.type).coerceAtLeast(0)],
                typeLabels,
                { change(row.copy(type = types[it])) },
                enabled = enabled,
            )
            val other = stringResource(R.string.pulse_horizon_account_other)
            PulseSelectionField(
                stringResource(R.string.pulse_horizon_statement_category),
                categories.firstOrNull { it.id == row.categoryId }?.name ?: other,
                listOf(other) + categories.map(HorizonCategory::name),
                { change(row.copy(categoryId = categories.getOrNull(it - 1)?.id)) },
                enabled = enabled,
            )
        }
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
                    if (result.size() + size > MaxPdfBytes) return@withContext null
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
