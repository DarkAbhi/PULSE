package dev.abhishekan.pulse.feature.horizon

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.net.URI

internal fun validPurchaseLink(value: String): Boolean {
    if (value.isBlank()) return true
    val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
    return uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank()
}

internal fun validPurchaseName(value: String): Boolean = value.trim().let { it.isNotEmpty() && it.codePointCount(0, it.length) <= 160 }

internal fun validPurchaseAmount(value: String): Boolean =
    value.toBigDecimalOrNull()?.let {
        it.signum() >= 0 && it <= "9999999999.99".toBigDecimal() && it.stripTrailingZeros().scale() <= 2
    } == true

/** Display-only grouping keeps the draft, selection and backend value free of separators. */
internal class PurchaseAmountTransformation(
    private val indianGrouping: Boolean,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val integerEnd = raw.indexOf('.').let { if (it < 0) raw.length else it }
        val boundaries =
            (1 until integerEnd)
                .filter { index ->
                    val remaining = integerEnd - index
                    if (indianGrouping) remaining == 3 || (remaining > 3 && (remaining - 3) % 2 == 0) else remaining % 3 == 0
                }.toSet()
        val rendered =
            buildString {
                raw.forEachIndexed { index, char ->
                    if (index in boundaries) append(',')
                    append(char)
                }
            }
        return TransformedText(
            AnnotatedString(rendered),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset + boundaries.count { it <= offset }

                override fun transformedToOriginal(offset: Int): Int = rendered.take(offset).count { it != ',' }
            },
        )
    }
}
