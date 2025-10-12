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
        minSdk = 29
        targetSdk = 35
        versionCode = 21
        versionName = "2.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

tasks.register<Exec>("runNdkBuild") {
    group = "build"

    val ndkDir = android.ndkDirectory
    executable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        "$ndkDir\\ndk-build.cmd"
    } else {
        "$ndkDir/ndk-build"
    }
    setArgs(listOf(
        "NDK_PROJECT_PATH=build/intermediates/ndkBuild",
        "NDK_LIBS_OUT=src/main/jniLibs",
        "APP_BUILD_SCRIPT=src/main/jni/Android.mk",
        "NDK_APPLICATION_MK=src/main/jni/Application.mk"
    ))

    println("Command: $commandLine")
}

tasks.preBuild {
    dependsOn("runNdkBuild")
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