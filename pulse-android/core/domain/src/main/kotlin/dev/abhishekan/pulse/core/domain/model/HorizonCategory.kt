package dev.abhishekan.pulse.core.domain.model

data class HorizonCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val color: String,
    val isDefault: Boolean,
    val parentId: Long? = null,
    val allowsSubcategories: Boolean = false,
    val isSubscriptionDefault: Boolean = false,
    val displayName: String = name,
)
