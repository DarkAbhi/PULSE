plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val releaseApiUrl = providers.gradleProperty("pulse.releaseApiUrl").orElse("https://pulse.invalid/api/")
val debugApiUrl = providers.gradleProperty("pulse.debugApiUrl").orElse("http://10.0.2.2:18081/api/")
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "dev.abhishekan.pulse"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "dev.abhishekan.pulse"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug { buildConfigField("String", "API_BASE_URL", quoted(debugApiUrl.get())) }
        release {
            buildConfigField("String", "API_BASE_URL", quoted(releaseApiUrl.get()))
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions { unitTests.all { it.systemProperty("pulse.root", rootProject.projectDir.absolutePath) } }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:network"))
    implementation(project(":core:ui"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:fitness"))
    implementation(project(":feature:horizon"))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.koin.test)
    testImplementation(libs.ktor.core)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.ktor.core)
    androidTestImplementation(libs.ktor.mock)
    androidTestImplementation(libs.serialization.json)
    androidTestImplementation(libs.coroutines.test)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}