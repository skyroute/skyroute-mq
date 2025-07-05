plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("skyroute-spotless")
}

android {
    namespace = "com.skyroute.example"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.skyroute.example"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(project(":sky-api"))
    implementation(project(":sky-core"))
    implementation(project(":sky-service"))

    implementation(libs.androidx.lifecycle.viewmodel)
}
