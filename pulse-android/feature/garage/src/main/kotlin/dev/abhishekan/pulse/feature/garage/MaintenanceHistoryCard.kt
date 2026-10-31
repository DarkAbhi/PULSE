package dev.abhishekan.pulse.feature.garage

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.abhishekan.pulse.core.domain.model.GarageChange
import dev.abhishekan.pulse.core.domain.model.GarageReceipt
import dev.abhishekan.pulse.core.domain.model.GarageRecord
import dev.abhishekan.pulse.core.domain.model.Maintenance
import dev.abhishekan.pulse.core.domain.model.UserCurrency
import dev.abhishekan.pulse.core.ui.PulseTheme
import dev.abhishekan.pulse.core.ui.PulseTokens
import dev.abhishekan.pulse.core.ui.UiText
import dev.abhishekan.pulse.core.ui.components.PulseButton
import dev.abhishekan.pulse.core.ui.components.PulseButtonStyle
import dev.abhishekan.pulse.core.ui.components.PulseButtonVariant
import dev.abhishekan.pulse.core.ui.components.PulseCard
import dev.abhishekan.pulse.core.ui.components.PulseText
import dev.abhishekan.pulse.core.ui.components.PulseTextStyle
import dev.abhishekan.pulse.core.ui.components.PulseTransferProgress
import dev.abhishekan.pulse.core.ui.pulseDescription
import dev.abhishekan.pulse.core.ui.uiText

@Composable
internal fun MaintenanceHistoryCard(
    item: Maintenance,
    vehicle: Long,
    enabled: Boolean,
    busy: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
    upload: (Long) -> Unit,
    open: (Long, GarageReceipt) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
    uploadRecord: Long?,
    progress: Float?,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().testTag("garage-care-${item.id}")) {
        val sideBySide = maxWidth >= 840.dp && LocalDensity.current.fontScale <= 1.3f
        val summary: @Composable () -> Unit = {
            MaintenancePanel { MaintenanceSummary(item, vehicle, enabled, edit, remove) }
        }
        val receipts: @Composable () -> Unit = {
            MaintenancePanel {
                MaintenanceReceipts(item, vehicle, enabled, busy, remove, upload, open, download, uploadRecord, progress)
            }
        }
        if (sideBySide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                Box(Modifier.weight(4f)) { summary() }
                Box(Modifier.weight(5f)) { receipts() }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
                summary()
                receipts()
            }
        }
    }
}

@Composable
private fun MaintenancePanel(content: @Composable () -> Unit) {
    PulseCard(
        Modifier.fillMaxWidth(),
        border = BorderStroke(PulseTokens.focusWidth / 2, PulseTheme.colors.divider),
        shape = PulseTheme.shapes.extraLarge,
    ) { content() }
}

@Composable
private fun MaintenanceSummary(
    item: Maintenance,
    vehicle: Long,
    enabled: Boolean,
    edit: (GarageDraft) -> Unit,
    remove: (GarageChange.Delete, UiText) -> Unit,
) {
    val colors = PulseTheme.colors
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.medium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.medium), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(PulseTokens.target).background(colors.errorSoft, PulseTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) { MaintenanceIcon(R.drawable.garage_service, colors.coral) }
            PulseText(item.title, Modifier.weight(1f).semantics { heading() }, PulseTextStyle.Heading3)
        }
        PulseText(UserCurrency.format(item.amount), style = PulseTextStyle.DisplayXL, color = colors.action)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            MaintenanceBadge(garageOption(item.category), accented = true)
            MaintenanceBadge(garageDate(item.date), R.drawable.garage_calendar)
            item.odometer?.let {
                MaintenanceBadge(
                    stringResource(R.string.pulse_garage_distance_km, decimal(it)),
                    R.drawable.garage_odometer,
                )
            }
        }
        item.provider?.takeIf { it.isNotBlank() }?.let { PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary) }
        item.notes?.takeIf { it.isNotBlank() }?.let { PulseText(it, style = PulseTextStyle.Body2, color = colors.textSecondary) }
        MaintenanceDivider()
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PulseButton(
                stringResource(R.string.pulse_garage_edit),
                { edit(item.draft(vehicle)) },
                Modifier.pulseDescription(stringResource(R.string.pulse_garage_edit_1_s, item.title)),
                enabled,
                variant = PulseButtonVariant.Secondary,
                leadingIcon = { MaintenanceIcon(R.drawable.garage_edit) },
            )
            PulseButton(
                stringResource(R.string.pulse_garage_delete),
                {
                    remove(
                        GarageChange.Delete(GarageRecord.Maintenance, vehicle, item.id),
                        uiText(R.string.pulse_garage_delete_1_s_this_permanently_removes_the_maintenance_record, item.title),
                    )
                },
                Modifier.pulseDescription(stringResource(R.string.pulse_garage_delete_1_s, item.title)),
                enabled,
                variant = PulseButtonVariant.Destructive,
                style = PulseButtonStyle.Text,
                leadingIcon = { GarageDetailIcon(GarageIcon.Delete) },
            )
        }
    }
}

@Composable
private fun MaintenanceReceipts(
    item: Maintenance,
    vehicle: Long,
    enabled: Boolean,
    busy: Boolean,
    remove: (GarageChange.Delete, UiText) -> Unit,
    upload: (Long) -> Unit,
    open: (Long, GarageReceipt) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
    uploadRecord: Long?,
    progress: Float?,
) {
    val colors = PulseTheme.colors
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(PulseTokens.small),
        ) {
            PulseText(
                stringResource(R.string.pulse_garage_receipts_count, item.receipts.size),
                Modifier.semantics { heading() }.padding(vertical = PulseTokens.small),
                PulseTextStyle.Title2,
            )
            PulseButton(
                stringResource(R.string.pulse_garage_attach),
                { upload(item.id) },
                Modifier.pulseDescription(stringResource(R.string.pulse_garage_attach_receipt_to_1_s, item.title)),
                enabled,
                variant = PulseButtonVariant.Soft,
                leadingIcon = { MaintenanceIcon(R.drawable.garage_attach) },
            )
        }
        if (item.receipts.isEmpty()) {
            PulseText(
                stringResource(R.string.pulse_garage_no_receipts_attached),
                style = PulseTextStyle.Body2,
                color = colors.textSecondary,
            )
        }
        Column {
            val visibleReceipts = if (expanded) item.receipts else item.receipts.take(3)
            visibleReceipts.forEachIndexed { index, receipt ->
                key(receipt.id) {
                    if (index > 0) MaintenanceDivider()
                    MaintenanceReceiptRow(receipt, item.id, vehicle, enabled, busy, remove, open, download)
                }
            }
        }
        if (item.receipts.size > 3) {
            PulseButton(
                if (expanded) {
                    stringResource(R.string.pulse_garage_show_fewer_documents)
                } else {
                    stringResource(R.string.pulse_garage_show_all_documents, item.receipts.size)
                },
                { expanded = !expanded },
                variant = PulseButtonVariant.Secondary,
                style = PulseButtonStyle.Text,
            )
        }
        if (uploadRecord == item.id && progress != null) PulseTransferProgress(progress)
        Row(horizontalArrangement = Arrangement.spacedBy(PulseTokens.small)) {
            MaintenanceIcon(R.drawable.garage_info, colors.textSecondary)
            PulseText(
                stringResource(R.string.pulse_garage_up_to_10_mb_per_file_your_saved_record),
                Modifier.weight(1f),
                PulseTextStyle.Caption,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MaintenanceReceiptRow(
    receipt: GarageReceipt,
    record: Long,
    vehicle: Long,
    enabled: Boolean,
    busy: Boolean,
    remove: (GarageChange.Delete, UiText) -> Unit,
    open: (Long, GarageReceipt) -> Unit,
    download: (Long, GarageReceipt) -> Unit,
) {
    val colors = PulseTheme.colors
    Row(
        Modifier.fillMaxWidth().testTag("garage-receipt-${receipt.id}").padding(vertical = PulseTokens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(enabled = !busy, onClickLabel = stringResource(R.string.pulse_garage_receipt_open)) { open(record, receipt) }
                .pulseDescription(stringResource(R.string.pulse_garage_open_1_s, receipt.name))
                .padding(vertical = PulseTokens.small),
            horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(32.dp).background(colors.infoSoft, PulseTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) { MaintenanceIcon(R.drawable.garage_document, colors.info) }
            Column(Modifier.weight(1f)) {
                PulseText(receipt.name, style = PulseTextStyle.Label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                PulseText(
                    Formatter.formatShortFileSize(LocalContext.current, receipt.size),
                    style = PulseTextStyle.Caption,
                    color = colors.textSecondary,
                )
            }
        }
        PulseButton(
            stringResource(R.string.pulse_garage_download_1_s, receipt.name),
            { download(record, receipt) },
            enabled = !busy,
            style = PulseButtonStyle.Icon,
            icon = { MaintenanceIcon(R.drawable.garage_download) },
        )
        PulseButton(
            stringResource(R.string.pulse_garage_remove_1_s, receipt.name),
            {
                remove(
                    GarageChange.Delete(GarageRecord.Receipt, vehicle, receipt.id, record),
                    uiText(R.string.pulse_garage_permanently_remove_1_s_from_server_storage_and_this, receipt.name),
                )
            },
            enabled = enabled,
            style = PulseButtonStyle.Icon,
            icon = { GarageDetailIcon(GarageIcon.Delete) },
        )
    }
}

@Composable
private fun MaintenanceBadge(
    label: String,
    icon: Int? = null,
    accented: Boolean = false,
) {
    val colors = PulseTheme.colors
    Row(
        Modifier
            .background(if (accented) colors.primarySoft else colors.surfaceMuted, PulseTheme.shapes.pill)
            .border(
                BorderStroke(PulseTokens.focusWidth / 2, if (accented) colors.action.copy(alpha = .25f) else colors.divider),
                PulseTheme.shapes.pill,
            ).padding(horizontal = PulseTheme.spacing.compact, vertical = PulseTokens.small),
        horizontalArrangement = Arrangement.spacedBy(PulseTokens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) MaintenanceIcon(icon, colors.textSecondary)
        PulseText(label, style = PulseTextStyle.Label2, color = if (accented) colors.action else colors.textSecondary)
    }
}

@Composable
private fun MaintenanceDivider() {
    Box(Modifier.fillMaxWidth().height(PulseTokens.focusWidth / 2).background(PulseTheme.colors.divider))
}

@Composable
private fun MaintenanceIcon(
    icon: Int,
    tint: Color = LocalContentColor.current,
) {
    Icon(painterResource(icon), null, Modifier.size(PulseTokens.medium), tint = tint)
}
