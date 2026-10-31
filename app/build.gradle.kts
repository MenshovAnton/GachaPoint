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
        versionCode = 10
        versionName = "0.3.0"

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
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    implementation(libs.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    annotationProcessor(libs.room.compiler)
}
