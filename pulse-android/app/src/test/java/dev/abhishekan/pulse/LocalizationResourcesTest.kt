package dev.abhishekan.pulse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationResourcesTest {
    private val root = File(checkNotNull(System.getProperty("pulse.root")))
    private val modules =
        listOf(
            "app",
            "core/ui",
            "feature/auth",
            "feature/home",
            "feature/fitness",
            "feature/horizon",
            "feature/garage",
            "feature/mealplan",
            "feature/profile",
            "feature/notifications",
        )

    @Test fun libraryStringsCannotOverrideOtherFeaturesWhenAndroidMergesResources() {
        val owners = mutableMapOf<String, String>()
        modules.forEach { module ->
            val document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    root.resolve("$module/src/main/res/values/strings.xml"),
                )
            val children = document.documentElement.childNodes
            for (i in 0 until children.length) {
                val item = children.item(i) as? Element ?: continue
                val name = item.getAttribute("name")
                assertNull(
                    "Resource $name collides between $module and ${owners[name]}",
                    owners.put(name, module),
                )
                if (name !=
                    "app_name"
                ) {
                    assertTrue(
                        "$module needs a unique string prefix: $name",
                        name.startsWith("pulse_${module.substringAfterLast('/')}_"),
                    )
                }
            }
        }
    }

    @Test fun formattedTranslationsUseNumberedArgumentsAndValidFormatStrings() {
        modules.forEach { module ->
            val document =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    root.resolve("$module/src/main/res/values/strings.xml"),
                )
            listOf("string", "item").forEach { tag ->
                val entries = document.getElementsByTagName(tag)
                for (index in 0 until entries.length) {
                    val value = entries.item(index).textContent
                    assertFalse(
                        "Unnumbered placeholder in $module: $value",
                        Regex("(?<!%)%(?:s|d|f)").containsMatchIn(value),
                    )
                    try {
                        validateFormatString(value)
                    } catch (error: IllegalArgumentException) {
                        throw AssertionError("Invalid format in $module: $value", error)
                    }
                }
            }
        }
    }

    @Test fun formatValidationSupportsMixedAndNumericOnlyArguments() {
        validateFormatString("Step %1\$d of 3 · %2\$s")
        validateFormatString("%1\$d matches")
        validateFormatString("%1\$.2f · %2\$s · %1\$.1f")
        validateFormatString("%1\$s · 100%%")
        validateFormatString("%1\$d · %1\$s")
    }

    @Test(expected = java.util.IllegalFormatConversionException::class)
    fun formatValidationRejectsConflictingArgumentTypes() {
        validateFormatString("%1\$d · %1\$f")
    }

    private fun validateFormatString(value: String) {
        // Consume escaped percent signs before collecting typed, numbered arguments.
        val placeholders =
            Regex("%%|%([0-9]+)[$][-#+ 0,(]*[0-9]*(?:\\.[0-9]+)?([sdf])")
                .findAll(value)
                .filter { it.value != "%%" }
                .toList()
        if (placeholders.isEmpty()) return
        val arguments = Array<Any>(placeholders.maxOf { it.groupValues[1].toInt() }) { "sample" }
        placeholders.forEach {
            val index = it.groupValues[1].toInt() - 1
            when (it.groupValues[2]) {
                "d" -> arguments[index] = 1L
                "f" -> arguments[index] = 1.5
            }
        }
        String.format(Locale.ROOT, value, *arguments)
    }

    @Test fun productionPulseControlsDoNotEmbedEnglishCopy() {
        val literal =
            Regex(
                "Pulse(?:Text|TextButton|Button|IconButton|TopAppBar|EmptyState|ErrorState|LoadingState|Dialog|FilterChip)\\s*\\(\\s*\"",
            )
        modules.forEach { module ->
            root
                .resolve("$module/src/main")
                .walkTopDown()
                .filter {
                    it.extension ==
                        "kt"
                }.forEach { file ->
                    assertFalse(
                        "Move visible copy to strings.xml: $file",
                        literal.containsMatchIn(file.readText()),
                    )
                }
        }
    }
}
