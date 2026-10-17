plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing material is supplied only by the developer/CI environment. Never
// commit keystores, passwords or an auto-generated production signing identity.
val storePath = System.getenv("GRIDBLAST_RELEASE_KEYSTORE_FILE")
val storePasswordEnv = System.getenv("GRIDBLAST_RELEASE_STORE_PASSWORD")
val keyAliasEnv = System.getenv("GRIDBLAST_RELEASE_KEY_ALIAS")
val keyPasswordEnv = System.getenv("GRIDBLAST_RELEASE_KEY_PASSWORD")
val releaseSigningAvailable = listOf(
    storePath, storePasswordEnv, keyAliasEnv, keyPasswordEnv,
).all { !it.isNullOrBlank() }

android {
    namespace = "tv.prohibited.gridblast"
    compileSdk = 37

    defaultConfig {
        applicationId = "tv.prohibited.gridblast"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    if (releaseSigningAvailable) {
        signingConfigs {
            create("gridblastRelease") {
                storeFile = file(requireNotNull(storePath))
                storePassword = requireNotNull(storePasswordEnv)
                keyAlias = requireNotNull(keyAliasEnv)
                keyPassword = requireNotNull(keyPasswordEnv)
            }
        }
    }
    buildTypes {
        getByName("release") {
            // Start with an unminified release while accessibility and device
            // testing remain in progress. Add shrinker testing before 1.0.
            isMinifyEnabled = false
            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("gridblastRelease")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
