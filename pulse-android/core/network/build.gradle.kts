plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.abhishekan.pulse.core.network"
    compileSdk { version = release(37) }
    defaultConfig {
        minSdk = 28
        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.koin.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    implementation(libs.ktor.core)
    implementation(libs.ktor.logging)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.json)
    implementation(libs.serialization.json)
    testImplementation(libs.ktor.mock)
}
