package dev.abhishekan.pulse

import java.io.File
import org.junit.Assert.*
import org.junit.Test

class ArchitectureTest {
    private val root = File(checkNotNull(System.getProperty("pulse.root")))
    private val allowed = mapOf(
        "app" to setOf("core:domain", "core:data", "core:network", "core:ui", "feature:auth", "feature:home"),
        "core/domain" to emptySet(), "core/data" to setOf("core:domain", "core:network"),
        "core/network" to emptySet(), "core/ui" to emptySet(),
        "feature/auth" to setOf("core:domain", "core:ui"), "feature/home" to setOf("core:domain", "core:ui"),
    )

    @Test fun moduleDirectionsRemainAcyclic() {
        val pattern = Regex("project\\(\":([^\"]+)\"\\)")
        allowed.forEach { (path, dependencies) ->
            val actual = pattern.findAll(root.resolve("$path/build.gradle.kts").readText()).map { it.groupValues[1] }.toSet()
            assertEquals("Unexpected module edges in $path", dependencies, actual)
        }
    }

    @Test fun domainAndFeaturesKeepInfrastructureOut() {
        val domain = root.resolve("core/domain/src").walkTopDown().filter { it.extension == "kt" }.joinToString("\n") { it.readText() }
        val domainBuild = root.resolve("core/domain/build.gradle.kts").readText()
        listOf("libs.koin", "libs.ktor", "libs.androidx", "plugins.android").forEach {
            assertFalse("Domain declares framework dependency $it", domainBuild.contains(it))
        }
        listOf("import android.", "import androidx.", "import org.koin", "import io.ktor", "import java.io").forEach {
            assertFalse("Domain imports $it", domain.contains(it))
        }
        listOf("auth", "home").forEach { feature ->
            root.resolve("feature/$feature/src").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                listOf("core.data", "core.network", "io.ktor", "feature.${if (feature == "auth") "home" else "auth"}").forEach {
                    assertFalse("Infrastructure or sibling feature import in $file", file.readText().contains("import dev.abhishekan.pulse.$it") || file.readText().contains("import $it"))
                }
            }
        }
    }

    @Test fun sessionBackupAndNetworkPoliciesStayRestricted() {
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"false\""))
        val release = root.resolve("app/src/main/res/xml/network_security_config.xml").readText()
        assertFalse(release.contains("cleartextTrafficPermitted=\"true\""))
        val debug = root.resolve("app/src/debug/res/xml/network_security_config.xml").readText()
        assertTrue(debug.contains("includeSubdomains=\"false\">10.0.2.2"))
        val storage = root.resolve("core/data/src/main/kotlin/dev/abhishekan/pulse/core/data/SessionStorage.kt").readText()
        assertTrue(storage.contains("context.noBackupFilesDir"))
    }
}
