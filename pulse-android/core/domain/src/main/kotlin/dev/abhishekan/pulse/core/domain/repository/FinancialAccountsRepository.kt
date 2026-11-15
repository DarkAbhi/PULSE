package dev.abhishekan.pulse.core.domain.repository

import dev.abhishekan.pulse.core.domain.model.FinancialAccount
import dev.abhishekan.pulse.core.domain.model.Outcome

interface FinancialAccountsRepository {
    suspend fun accounts(includeArchived: Boolean = false): Outcome<List<FinancialAccount>>

    suspend fun saveAccount(account: FinancialAccount): Outcome<Unit>

    suspend fun archiveAccount(id: Long, archived: Boolean): Outcome<Unit>
}
