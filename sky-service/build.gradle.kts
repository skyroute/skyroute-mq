plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("skyroute-spotless")
    id("skyroute-maven-publish")
}

android {
    namespace = "com.skyroute.service"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {

    implementation(project(":sky-core"))

    implementation(libs.kotlin.reflection)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockito.junit)

    // Paho MQTT Client
    implementation(libs.paho.client)

    // BCProv
    implementation(libs.bcprov.jdk15to18)
    implementation(libs.bcpkix.jdk15to18)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
