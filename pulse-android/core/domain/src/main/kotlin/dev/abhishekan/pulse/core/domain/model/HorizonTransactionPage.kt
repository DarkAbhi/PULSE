package dev.abhishekan.pulse.core.domain.model

data class HorizonTransactionPage(
    val items: List<HorizonTransaction>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)
