import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localConfig = Properties()
val localConfigFile = rootProject.file("local.properties")
if (localConfigFile.exists()) localConfigFile.inputStream().use { localConfig.load(it) }
val driveBackendUrl = localConfig.getProperty("walloraDriveBackendUrl")
    ?: System.getenv("WALLORA_DRIVE_BACKEND_URL")
    ?: "https://script.google.com/macros/s/AKfycbxIuQE5k78867sZKbFxFc20V8lTSfCGDGbZ8-DqbkQ_QgbzNqp337N0bsLcGCdZVharLA/exec"
val liveBannerAdUnitId = localConfig.getProperty("walloraBannerAdUnitId")
    ?: System.getenv("WALLORA_BANNER_AD_UNIT_ID")
    ?: "ca-app-pub-2494403833680298/4406989102"
val liveInterstitialAdUnitId = localConfig.getProperty("walloraInterstitialAdUnitId")
    ?: System.getenv("WALLORA_INTERSTITIAL_AD_UNIT_ID")
    ?: "ca-app-pub-2494403833680298/8578860333"
fun quotedBuildConfig(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.wallora.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wallora.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "DRIVE_BACKEND_URL", quotedBuildConfig(driveBackendUrl))
        buildConfigField("String", "LIVE_BANNER_AD_UNIT_ID", quotedBuildConfig(liveBannerAdUnitId))
        buildConfigField("String", "LIVE_INTERSTITIAL_AD_UNIT_ID", quotedBuildConfig(liveInterstitialAdUnitId))
    }
    sourceSets {
        getByName("main") {
            // Keep the 5.7 GB source archive out of the APK; the full collection is fetched from Drive.
            assets.setSrcDirs(emptyList<String>())
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.work:work-runtime-ktx:2.10.5")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.gms:play-services-ads-identifier:18.3.0")
}

