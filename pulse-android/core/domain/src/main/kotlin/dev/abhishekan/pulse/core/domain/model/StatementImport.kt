package dev.abhishekan.pulse.core.domain.model

data class StatementRow(
    val id: Int,
    val date: String,
    val description: String,
    val amount: String,
    val type: String,
    val categoryId: Long?,
    val included: Boolean,
    val duplicate: Boolean,
    val warnings: List<String> = emptyList(),
    val originalNarration: String = "",
)

data class StatementRows(
    val rows: List<StatementRow>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
)

data class StatementBatch(
    val id: String,
    val currency: String,
    val revision: Int,
    val ocrUsed: Boolean,
    val accountConfirmationRequired: Boolean,
    val parsed: Int,
    val duplicates: Int,
)

data class StatementPreview(
    val batch: StatementBatch,
    val rows: StatementRows,
)

data class StatementCommit(
    val imported: Int,
    val duplicates: Int,
    val excluded: Int,
)

sealed interface StatementResponse<out T> {
    data class Success<T>(
        val value: T,
    ) : StatementResponse<T>

    data class Rejected(
        val code: String,
    ) : StatementResponse<Nothing>

    data class Failed(
        val failure: Failure,
    ) : StatementResponse<Nothing>
}
