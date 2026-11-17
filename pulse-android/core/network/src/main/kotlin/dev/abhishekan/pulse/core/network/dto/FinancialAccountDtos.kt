package dev.abhishekan.pulse.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

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
    @SerialName("last_four") val lastFour: String,
    @SerialName("account_number") val accountNumber: String = "",
    @SerialName("holder_name") val holderName: String = "",
    @SerialName("holder_email") val holderEmail: String = "",
    @SerialName("card_type") val cardType: String = "",
    @SerialName("card_network") val cardNetwork: String = "",
    @SerialName("credit_limit") val creditLimit: JsonPrimitive? = null,
    @SerialName("statement_day") val statementDay: Int? = null,
    @SerialName("archived_at") val archivedAt: String? = null,
)
