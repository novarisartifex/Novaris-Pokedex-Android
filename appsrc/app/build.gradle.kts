plugins {
    id("com.android.application")
}

android {
    namespace = "com.novaris.pokedex.nativeapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.novaris.pokedex.nativeapp.v050"
        minSdk = 23
        targetSdk = 35
        versionCode = 150 + 100
        versionName = "0.5.11-native"
    }

    signingConfigs {
        create("novarisLocal") {
            storeFile = rootProject.file("novaris-local-signing.jks")
            storePassword = "novaris-local-2026"
            keyAlias = "novaris"
            keyPassword = "novaris-local-2026"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("novarisLocal")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("novarisLocal")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
