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

// Test-only signing identity for two successive DEBUG APK versions on one
// emulator. CI creates and destroys this key inside the same isolated job.
// It is never the owner's private beta or Google Play signing certificate.
val upgradeSmokeStorePath = System.getenv("GRIDBLAST_UPGRADE_CI_KEYSTORE_FILE")
val upgradeSmokePassword = System.getenv("GRIDBLAST_UPGRADE_CI_STORE_PASSWORD")
val upgradeSmokeAlias = System.getenv("GRIDBLAST_UPGRADE_CI_KEY_ALIAS")
val upgradeSmokeKeyPassword = System.getenv("GRIDBLAST_UPGRADE_CI_KEY_PASSWORD")
val upgradeSmokeFields = listOf(
    upgradeSmokeStorePath, upgradeSmokePassword,
    upgradeSmokeAlias, upgradeSmokeKeyPassword,
)
val upgradeSmokeRequested = upgradeSmokeFields.any { !it.isNullOrBlank() }
require(!upgradeSmokeRequested || upgradeSmokeFields.all { !it.isNullOrBlank() }) {
    "Incomplete test-only upgrade signing configuration"
}

// Public debug builds retain the stable source default. The private, manually
// dispatched beta channel supplies an increasing versionCode without committing
// an owner's signing identity or rewriting source files.
val distributionVersionCode = System.getenv("GRIDBLAST_VERSION_CODE")?.let { value ->
    require(value.matches(Regex("[1-9][0-9]{0,9}"))) {
        "GRIDBLAST_VERSION_CODE must be a positive decimal integer"
    }
    val code = value.toLong()
    require(code <= 2_100_000_000) {
        "GRIDBLAST_VERSION_CODE is outside the accepted Android Play version range"
    }
    code.toInt()
} ?: 1
val distributionVersionName = System.getenv("GRIDBLAST_VERSION_NAME")?.let { value ->
    require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,39}"))) {
        "GRIDBLAST_VERSION_NAME contains unsupported characters or is too long"
    }
    value
} ?: "0.1.0"

android {
    namespace = "tv.prohibited.gridblast"
    compileSdk = 37

    defaultConfig {
        applicationId = "tv.prohibited.gridblast"
        minSdk = 26
        targetSdk = 36
        versionCode = distributionVersionCode
        versionName = distributionVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    if (upgradeSmokeRequested) {
        signingConfigs {
            create("gridblastUpgradeCISmoke") {
                storeFile = file(requireNotNull(upgradeSmokeStorePath))
                storePassword = requireNotNull(upgradeSmokePassword)
                keyAlias = requireNotNull(upgradeSmokeAlias)
                keyPassword = requireNotNull(upgradeSmokeKeyPassword)
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (upgradeSmokeRequested) {
                // AndroidTest is built against this debug signing identity.
                signingConfig = signingConfigs.getByName("gridblastUpgradeCISmoke")
            }
        }
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
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
