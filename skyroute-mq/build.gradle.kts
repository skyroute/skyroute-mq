plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("skyroute-spotless")
    id("skyroute-maven-publish")
}

android {
    namespace = "com.skyroute.mq"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    api(project(":sky-api"))
    api(project(":sky-service"))
}
