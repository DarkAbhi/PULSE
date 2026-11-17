package dev.abhishekan.pulse.core.domain.model

data class FinancialAccount(
    val id: Long? = null,
    val displayName: String = "",
    val institutionName: String = "",
    val accountType: String = "bank_account",
    val bankAccountSubtype: String = "",
    val lastFour: String = "",
    val archived: Boolean = false,
    val accountNumber: String = "",
    val holderName: String = "",
    val holderEmail: String = "",
    val cardType: String = "",
    val cardNetwork: String = "",
    val creditLimit: String = "",
    val statementDay: String = "",
)
