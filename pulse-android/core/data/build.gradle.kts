plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.abhishekan.pulse.core.data"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 28; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.koin.core)
    implementation(libs.ktor.core)
    implementation(libs.serialization.json)
    testImplementation(libs.ktor.mock)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    implementation(project(":core:domain"))
    implementation(project(":core:network"))
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
}
