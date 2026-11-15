package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinancialAccountsDto(
    val accounts: List<FinancialAccountDto>,
)

@Serializable
data class FinancialAccountDto(
    val id: Long,
    @SerialName("display_name") val displayName: String,
    @SerialName("institution_name") val institutionName: String,
    @SerialName("account_type") val accountType: String,
    @SerialName("bank_account_subtype") val bankAccountSubtype: String,
    val currency: String,
    @SerialName("country_code") val countryCode: String,
    @SerialName("last_four") val lastFour: String,
    @SerialName("archived_at") val archivedAt: String? = null,
)
