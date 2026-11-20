package dev.abhishekan.pulse.feature.garage.detail.receipts

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.ui.uiText
import dev.abhishekan.pulse.feature.garage.R
import dev.abhishekan.pulse.feature.garage.detail.VehicleDetailAction
import dev.abhishekan.pulse.feature.garage.detail.VehicleDetailEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

internal class ReceiptPickerActions(
    val upload: (Long) -> Unit,
    val download: (Long, GarageReceipt) -> Unit,
)

private data class PendingDownload(
    val record: Long,
    val receipt: GarageReceipt,
)

private val pendingDownloadSaver =
    listSaver<PendingDownload?, Any>(
        save = { request ->
            request
                ?.let {
                    listOf(it.record, it.receipt.id, it.receipt.name, it.receipt.contentType, it.receipt.size)
                }.orEmpty()
        },
        restore = { values ->
            val (record, id, name, contentType, size) = values
            PendingDownload(record as Long, GarageReceipt(id as Long, name as String, contentType as String, size as Long))
        },
    )

@Composable
internal fun rememberReceiptPickerActions(
    events: Flow<VehicleDetailEvent>,
    onAction: (VehicleDetailAction) -> Unit,
): ReceiptPickerActions {
    val dispatch by rememberUpdatedState(onAction)

    fun reportError(
        @StringRes message: Int,
    ) {
        dispatch(VehicleDetailAction.FileError(uiText(message)))
    }

    var pendingUpload by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDownload by rememberSaveable(stateSaver = pendingDownloadSaver) { mutableStateOf<PendingDownload?>(null) }
    val uploadPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val record = pendingUpload
            pendingUpload = null
            if (uri != null && record != null) dispatch(VehicleDetailAction.UploadReceipt(record, uri.toString()))
        }
    val downloadPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            val request = pendingDownload
            pendingDownload = null
            if (uri != null && request != null) {
                dispatch(VehicleDetailAction.DownloadReceipt(request.record, request.receipt, uri.toString()))
            }
        }

    val context = LocalContext.current
    val title by rememberUpdatedState(stringResource(R.string.pulse_garage_open_receipt))
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(events, lifecycle, context) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            events.collectLatest { event ->
                when (event) {
                    is VehicleDetailEvent.OpenReceipt -> {
                        val intent =
                            Intent(Intent.ACTION_VIEW)
                                .setDataAndType(event.uri.toUri(), event.contentType)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            context.startActivity(Intent.createChooser(intent, title))
                        } catch (_: ActivityNotFoundException) {
                            reportError(R.string.pulse_garage_no_app_can_open_this_receipt_use_download_to)
                        } catch (_: SecurityException) {
                            reportError(R.string.pulse_garage_the_receipt_could_not_be_opened_download_a_copy)
                        }
                    }
                }
            }
        }
    }

    return ReceiptPickerActions(
        upload = { record ->
            pendingUpload = record
            try {
                uploadPicker.launch(arrayOf("*/*"))
            } catch (_: ActivityNotFoundException) {
                pendingUpload = null
                reportError(R.string.pulse_garage_a_system_file_picker_is_unavailable_on_this_device)
            }
        },
        download = { record, receipt ->
            pendingDownload = PendingDownload(record, receipt)
            try {
                downloadPicker.launch(receipt.name)
            } catch (_: ActivityNotFoundException) {
                pendingDownload = null
                reportError(R.string.pulse_garage_a_system_document_picker_is_unavailable_on_this_device)
            }
        },
    )
}
