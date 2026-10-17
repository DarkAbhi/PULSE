package dev.abhishekan.pulse

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test
import org.w3c.dom.Element

class LocalizationResourcesTest {
    private val root = File(checkNotNull(System.getProperty("pulse.root")))
    private val modules = listOf("app", "core/ui", "feature/auth", "feature/home", "feature/fitness", "feature/horizon", "feature/garage", "feature/mealplan")
    @Test fun libraryStringsCannotOverrideOtherFeaturesWhenAndroidMergesResources() {
        val owners = mutableMapOf<String, String>()
        modules.forEach { module ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("$module/src/main/res/values/strings.xml"))
            val children = document.documentElement.childNodes
            for (i in 0 until children.length) {
                val item = children.item(i) as? Element ?: continue
                val name = item.getAttribute("name")
                assertNull("Resource $name collides between $module and ${owners[name]}", owners.put(name, module))
                if (name != "app_name") assertTrue("$module needs a unique string prefix: $name", name.startsWith("pulse_${module.substringAfterLast('/')}_"))
            }
        }
    }
    @Test fun formattedTranslationsUseNumberedArgumentsAndValidFormatStrings() {
        modules.forEach { module ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("$module/src/main/res/values/strings.xml"))
            listOf("string", "item").forEach { tag ->
                val entries = document.getElementsByTagName(tag)
                for (index in 0 until entries.length) {
                    val value = entries.item(index).textContent
                    assertFalse("Unnumbered placeholder in $module: $value", Regex("(?<!%)%(?:s|d|f)").containsMatchIn(value))
                    val arguments = Regex("%([0-9]+)[$]s").findAll(value).map { it.groupValues[1].toInt() }.toList()
                    if (arguments.isNotEmpty()) String.format(Locale.ROOT, value, *Array(arguments.max()) { "sample" })
                }
            }
        }
    }
    @Test fun productionPulseControlsDoNotEmbedEnglishCopy() {
        val literal = Regex("Pulse(?:Text|TextButton|Button|IconButton|TopAppBar|EmptyState|ErrorState|LoadingState|Dialog|FilterChip)\\s*\\(\\s*\"")
        modules.forEach { module -> root.resolve("$module/src/main").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            assertFalse("Move visible copy to strings.xml: $file", literal.containsMatchIn(file.readText()))
        } }
    }
}
