package dev.abhishekan.pulse.core.domain.model

data class FinancialAccount(
    val id: Long? = null,
    val displayName: String = "",
    val institutionName: String = "",
    val accountType: String = "bank_account",
    val bankAccountSubtype: String = "",
    val currency: String,
    val countryCode: String = "",
    val lastFour: String = "",
    val archived: Boolean = false,
)
