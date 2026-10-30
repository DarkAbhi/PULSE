package dev.abhishekan.pulse.feature.horizon

import androidx.compose.ui.text.AnnotatedString
import dev.abhishekan.pulse.core.domain.model.HorizonChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseInputTest {
    @Test fun groupingPreservesPrecisionAndIndianBoundaries() {
        val transform = PurchaseAmountTransformation(true)
        mapOf(
            "" to "",
            "25" to "25",
            "25000" to "25,000",
            "125000.50" to "1,25,000.50",
            "1234567890." to "1,23,45,67,890.",
            "0.00" to "0.00",
        ).forEach { (raw, grouped) -> assertEquals(grouped, transform.filter(AnnotatedString(raw)).text.text) }
        assertEquals("125,000.50", PurchaseAmountTransformation(false).filter(AnnotatedString("125000.50")).text.text)
    }

    @Test fun everyCursorPositionRoundTripsWithoutLeavingTextBounds() {
        for (indian in listOf(true, false)) {
            for (raw in listOf("", "1", "1234", "1234567.89", "1234567890.00")) {
                val result = PurchaseAmountTransformation(indian).filter(AnnotatedString(raw))
                for (offset in 0..raw.length) {
                    val mapped = result.offsetMapping.originalToTransformed(offset)
                    assertTrue(mapped in 0..result.text.length)
                    assertEquals(offset, result.offsetMapping.transformedToOriginal(mapped))
                }
                val mappedOffsets = (0..result.text.length).map { result.offsetMapping.transformedToOriginal(it) }
                assertTrue(mappedOffsets.all { it in 0..raw.length })
                assertEquals(mappedOffsets.sorted(), mappedOffsets)
            }
        }
    }

    @Test fun optionalLinkAcceptsOnlyAbsoluteWebLinksWhenProvided() {
        listOf(
            "",
            "  ",
            "https://example.com/item?q=a%20b#detail",
            "http://example.com",
            " HTTPS://example.com/ ",
            "https://example.com/" + "a".repeat(4000),
        ).forEach { assertTrue(it, validPurchaseLink(it)) }
        listOf("example.com", "https://", "ftp://example.com", "javascript:alert(1)", "https://example.com/a b", "https:///item")
            .forEach { assertFalse(it, validPurchaseLink(it)) }
    }

    @Test fun validationMatchesExistingPurchaseLimitsIncludingZeroAndTrailingZeroes() {
        listOf("0", "0.00", "25000.50", "9999999999.99", "1.000").forEach { assertTrue(it, validPurchaseAmount(it)) }
        listOf("", ".", "-1", "1.001", "10000000000", "NaN").forEach { assertFalse(it, validPurchaseAmount(it)) }
        assertFalse(validPurchaseName("  "))
        assertTrue(validPurchaseName("a".repeat(160)))
        assertFalse(validPurchaseName("a".repeat(161)))
        assertTrue(validPurchaseName("😀".repeat(160)))
    }

    @Test fun draftPersistsRawAmountAndCompleteOptionalUrl() {
        val draft =
            HorizonDraft(
                HorizonForm.Purchase,
                name = "Monitor",
                amount = "125000.50",
                url =
                    "https://example.com/" + "a".repeat(3000),
                dirty = true,
            )
        assertEquals(draft, HorizonDraft.restore(draft.saved()))
        val change = draft.change() as HorizonChange.Purchase
        assertEquals("125000.50".toBigDecimal(), change.amount)
        assertEquals(draft.url, change.url)
        assertEquals(null, (draft.copy(url = " ").change() as HorizonChange.Purchase).url)
    }
}
