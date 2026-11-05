plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.jondude.stardustranch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jondude.stardustranch"
        minSdk = 26
        targetSdk = 35
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    signingConfigs {
        create("stardustranch") {
            storeFile = rootProject.file("keystore/stardustranch.jks")
            storePassword = "stardustranch"
            keyAlias = "stardustranch"
            keyPassword = "stardustranch"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stardustranch")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("stardustranch")
        }
    }

    buildFeatures { buildConfig = true }

    lint { checkReleaseBuilds = false }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
