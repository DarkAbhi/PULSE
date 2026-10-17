package dev.abhishekan.pulse.core.ui

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Resolve at display time so retained UI messages follow the current language. */
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val arguments: List<Any?> = emptyList()) : UiText
    data class Plural(@param:PluralsRes val id: Int, val quantity: Int, val arguments: List<Any?> = emptyList()) : UiText
    data class Literal(val value: String) : UiText
}
fun uiText(@StringRes id: Int, vararg arguments: Any?): UiText = UiText.Resource(id, arguments.toList())
fun uiPlural(@PluralsRes id: Int, quantity: Int, vararg arguments: Any?): UiText = UiText.Plural(id, quantity, arguments.toList())
fun UiText.resolve(resources: Resources): String {
    fun List<Any?>.resolved() = map { if (it is UiText) it.resolve(resources) else it }.toTypedArray()
    return when (this) {
        is UiText.Resource -> if (arguments.isEmpty()) resources.getString(id) else resources.getString(id, *arguments.resolved())
        is UiText.Plural -> if (arguments.isEmpty()) resources.getQuantityString(id, quantity) else resources.getQuantityString(id, quantity, *arguments.resolved())
        is UiText.Literal -> value
    }
}
@Composable fun UiText.asString(): String = resolve(LocalResources.current)

fun Modifier.pulseDescription(text: String): Modifier = semantics { contentDescription = text }
