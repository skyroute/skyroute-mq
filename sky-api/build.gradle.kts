plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("skyroute-spotless")
    id("skyroute-maven-publish")
}

android {
    namespace = "com.skyroute.api"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
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
}

dependencies {

    implementation(project(":sky-core"))
    implementation(project(":sky-service"))

    implementation(libs.kotlin.reflection)
    implementation(libs.androidx.core.ktx)

    testImplementation(project(":sky-test"))
    testImplementation(libs.junit)
}
