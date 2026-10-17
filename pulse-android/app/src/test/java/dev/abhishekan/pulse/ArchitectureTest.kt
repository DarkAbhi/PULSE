package dev.abhishekan.pulse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureTest {
    private val root = File(checkNotNull(System.getProperty("pulse.root")))
    private val allowed =
        mapOf(
            "app" to
                setOf(
                    "core:domain",
                    "core:data",
                    "core:network",
                    "core:ui",
                    "feature:auth",
                    "feature:home",
                    "feature:fitness",
                    "feature:horizon",
                    "feature:garage",
                    "feature:mealplan",
                ),
            "core/domain" to emptySet(),
            "core/data" to setOf("core:domain", "core:network"),
            "core/network" to emptySet(),
            "core/ui" to setOf("core:domain"),
            "feature/mealplan" to setOf("core:domain", "core:ui"),
            "feature/garage" to setOf("core:domain", "core:ui"),
            "feature/horizon" to setOf("core:domain", "core:ui"),
            "feature/fitness" to setOf("core:domain", "core:ui"),
            "feature/auth" to setOf("core:domain", "core:ui"),
            "feature/home" to setOf("core:domain", "core:ui"),
        )

    @Test fun moduleDirectionsRemainAcyclic() {
        val pattern = Regex("project\\(\":([^\"]+)\"\\)")
        allowed.forEach { (path, dependencies) ->
            val actual =
                pattern
                    .findAll(root.resolve("$path/build.gradle.kts").readText())
                    .map {
                        it.groupValues[1]
                    }.toSet()
            assertEquals("Unexpected module edges in $path", dependencies, actual)
        }
    }

    @Test fun domainAndFeaturesKeepInfrastructureOut() {
        val domain =
            root
                .resolve("core/domain/src")
                .walkTopDown()
                .filter {
                    it.extension == "kt"
                }.joinToString("\n") { it.readText() }
        val domainBuild = root.resolve("core/domain/build.gradle.kts").readText()
        listOf("libs.koin", "libs.ktor", "libs.androidx", "plugins.android").forEach {
            assertFalse("Domain declares framework dependency $it", domainBuild.contains(it))
        }
        listOf(
            "import android.",
            "import androidx.",
            "import org.koin",
            "import io.ktor",
            "import java.io",
        ).forEach {
            assertFalse("Domain imports $it", domain.contains(it))
        }
        listOf("auth", "home", "fitness", "horizon", "garage", "mealplan").forEach { feature ->
            root
                .resolve("feature/$feature/src")
                .walkTopDown()
                .filter {
                    it.extension == "kt"
                }.forEach { file ->
                    (
                        listOf("core.data", "core.network", "io.ktor") +
                            listOf("auth", "home", "fitness", "horizon", "garage", "mealplan")
                                .filter {
                                    it !=
                                        feature
                                }.map { "feature.$it" }
                    ).forEach {
                        assertFalse(
                            "Infrastructure or sibling feature import in $file",
                            file.readText().contains("import dev.abhishekan.pulse.$it") ||
                                file.readText().contains("import $it"),
                        )
                    }
                }
        }
    }

    @Test fun productionConcurrencyAndInjectionKeepTheirBoundaries() {
        allowed.keys.forEach { module ->
            root
                .resolve("$module/src")
                .listFiles()
                .orEmpty()
                .filter { it.name in setOf("main", "debug", "release") }
                .flatMap { source -> source.walkTopDown().filter { it.extension == "kt" }.toList() }
                .forEach { file ->
                    val source = file.readText()
                    assertFalse("Unscoped coroutine in $file", source.contains("GlobalScope"))
                    val implementation =
                        file.name.endsWith("ViewModel.kt") ||
                            module == "core/domain" ||
                            (module == "core/data" && file.parentFile?.name != "di")
                    if (implementation) {
                        assertFalse(
                            "Service locator import in $file",
                            source.contains("import org.koin"),
                        )
                    }
                    if (file.name.endsWith("ViewModel.kt")) {
                        listOf(
                            "io.ktor",
                            "java.io",
                            "dev.abhishekan.pulse.core.data",
                            "dev.abhishekan.pulse.core.network",
                        ).forEach {
                            assertFalse(
                                "Infrastructure import in $file",
                                source.contains("import $it"),
                            )
                        }
                        assertFalse(
                            "Public mutable state in $file",
                            Regex(
                                "(?m)^    (?:override )?val \\w+(?:\\s*:[^=\\n]+)?\\s*=\\s*MutableStateFlow",
                            ).containsMatchIn(source),
                        )
                    }
                }
        }
    }

    @Test fun receiptSharingStaysInsidePrivateCacheWithoutBroadStoragePermissions() {
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").readText()
        assertTrue(
            manifest.contains("android:exported=\"false\" android:grantUriPermissions=\"true\""),
        )
        listOf(
            "READ_EXTERNAL_STORAGE",
            "WRITE_EXTERNAL_STORAGE",
            "MANAGE_EXTERNAL_STORAGE",
        ).forEach {
            assertFalse(manifest.contains(it))
        }
        val paths = root.resolve("app/src/main/res/xml/receipt_paths.xml").readText()
        assertTrue(paths.contains("path=\"receipts/\""))
        assertFalse(paths.contains("root-path"))
        assertFalse(paths.contains("external-path"))
    }

    @Test fun sessionBackupAndNetworkPoliciesStayRestricted() {
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"false\""))
        val release = root.resolve("app/src/main/res/xml/network_security_config.xml").readText()
        assertFalse(release.contains("cleartextTrafficPermitted=\"true\""))
        val debug = root.resolve("app/src/debug/res/xml/network_security_config.xml").readText()
        assertTrue(debug.contains("includeSubdomains=\"false\">10.0.2.2"))
        val storage =
            root
                .resolve(
                    "core/data/src/main/kotlin/dev/abhishekan/pulse/core/data/SessionStorage.kt",
                ).readText()
        assertTrue(storage.contains("context.noBackupFilesDir"))
    }
}
