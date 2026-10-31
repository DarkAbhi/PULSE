package dev.abhishekan.pulse.core.domain.model

data class Profile(
    val name: String?,
    val hasProfile: Boolean = true,
    val timezone: String = "UTC",
    val currency: String = UserCurrency.DEFAULT,
)
