plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "li.gkd.benchmark"
    compileSdk = 37
    compileSdkMinor = 2
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    // The benchmark test module instruments the perf variant of the app, which is the only
    // variant that is both R8-optimised and signed with a key we actually hold.
    targetProjectPath = ":gkd-app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // Test modules must declare the same flavours as the app they target.
    flavorDimensions += "channel"
    productFlavors {
        create("gkd") {
            dimension = "channel"
        }
        create("play") {
            dimension = "channel"
        }
    }

    buildTypes {
        // Mirrors the app's perf build type so the two variants line up.
        create("perf") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
        }
    }
}

baselineProfile {
    // Run against whatever is attached over adb instead of Gradle managed devices.
    useConnectedDevices = true
    // The only device available here is an emulator; the profile is still valid, it just needs a
    // plain `connected` run rather than a benchmark-grade one.
    skipBenchmarksOnEmulator = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
}
