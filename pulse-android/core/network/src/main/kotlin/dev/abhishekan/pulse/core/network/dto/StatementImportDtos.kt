package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StatementRowDto(
    val id: Int,
    val date: String,
    val description: String,
    val amount: String,
    val type: String,
    @SerialName("category_id") val categoryId: Long?,
    val included: Boolean,
    @SerialName("duplicate_status") val duplicateStatus: String,
    val warnings: List<String> = emptyList(),
    @SerialName("original_narration") val originalNarration: String = "",
)

@Serializable
data class StatementRowsDto(
    val rows: List<StatementRowDto>,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val total: Int,
)

@Serializable
data class StatementBatchDto(
    val id: String,
    val currency: String,
    val revision: Int,
    @SerialName("ocr_used") val ocrUsed: Boolean,
    @SerialName("account_confirmation_required") val accountConfirmationRequired: Boolean,
    val counts: StatementCountsDto,
    val rows: List<StatementRowDto> = emptyList(),
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 100,
    val total: Int = 0,
)

@Serializable
data class StatementCountsDto(
    val parsed: Int,
    val duplicates: Int,
)

@Serializable
data class StatementCommitDto(
    val imported: Int,
    @SerialName("duplicates_skipped") val duplicatesSkipped: Int,
    val excluded: Int,
)

@Serializable
data class StatementErrorDto(
    val code: String = "",
)
