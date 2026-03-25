plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    kotlin("plugin.serialization") version "2.1.20"
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.6"
}

android {
    namespace = "com.cherret.zaprett"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cherret.zaprett"
        minSdk = 28
        targetSdk = 35
        versionCode = 27
        versionName = "2.15"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "send_firebase_analytics", "true")
            buildConfigField("boolean", "auto_update", "true")
        }
        debug {
            buildConfigField("boolean", "send_firebase_analytics", "false")
            buildConfigField("boolean", "auto_update", "false")
        }
    }
    externalNativeBuild {
        ndkBuild {
            path("src/main/jni/Android.mk")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

cargo {
    module  = "../rust"
    libname = "byedpi"
    targets = listOf("arm", "arm64", "x86", "x86_64")
    profile = "release"
}

tasks.preBuild {
    dependsOn("cargoBuild")
}

tasks.register<Exec>("cargoClean") {
    workingDir = file("../rust")
    commandLine("cargo", "clean")
    group = "build"
}

tasks.named("clean") {
    dependsOn("cargoClean")
}

dependencies {
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material3.adaptive.nav)
    implementation(libs.navigation.compose)
    implementation(libs.compose.icons)
    implementation(libs.libsu.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp)
    implementation(libs.serialization.json)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.fragment.compose)
    implementation(libs.coil.compose)
    implementation(libs.compose.markdown)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
