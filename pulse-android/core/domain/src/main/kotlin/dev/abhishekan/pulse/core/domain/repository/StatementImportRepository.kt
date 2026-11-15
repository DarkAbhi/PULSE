package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.StatementBatch
import dev.abhishekan.pulse.core.domain.model.StatementCommit
import dev.abhishekan.pulse.core.domain.model.StatementPreview
import dev.abhishekan.pulse.core.domain.model.StatementResponse
import dev.abhishekan.pulse.core.domain.model.StatementRow
import dev.abhishekan.pulse.core.domain.model.StatementRows

interface StatementImportRepository {
    suspend fun uploadStatement(
        accountId: Long,
        pdf: ByteArray,
        progress: (Float?) -> Unit,
    ): StatementResponse<StatementPreview>

    suspend fun statementBatch(id: String): StatementResponse<StatementBatch>

    suspend fun statementRows(
        id: String,
        page: Int,
    ): StatementResponse<StatementRows>

    suspend fun editStatement(
        batch: StatementBatch,
        confirmed: Boolean,
        changes: List<Pair<StatementRow, StatementRow>>,
    ): StatementResponse<StatementBatch>

    suspend fun commitStatement(batch: StatementBatch): StatementResponse<StatementCommit>

    suspend fun cancelStatement(id: String): StatementResponse<Unit>
}
