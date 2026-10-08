plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ru.menshovanton.gachapoint"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.menshovanton.gachapoint"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.gson)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.recyclerview)
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.splashscreen)
    implementation(libs.viewpager2)
    implementation(libs.work.runtime)
    implementation(libs.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    annotationProcessor(libs.room.compiler)
}