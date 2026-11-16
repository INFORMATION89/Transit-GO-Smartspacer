plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.marco.smartspacer.transitgo"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.marco.smartspacer.transitgo"
        minSdk = 29
        targetSdk = 36
        versionCode = 110
        versionName = "1.1"
    }

    buildTypes {
        release {
            // Shrinking strips the unused Material icons (the settings use 6 of thousands)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // No release key for this personal plugin: sign with the debug key so it installs
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation("com.kieronquinn.smartspacer:sdk-plugin:1.1.2")
    implementation("androidx.core:core-ktx:1.13.1")

    // Settings screen: Compose + Material 3 Expressive
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.material3:material3:1.5.0-rc01")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.11.0")
}
