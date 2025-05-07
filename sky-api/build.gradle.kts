import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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

    implementation(project(":sky-service"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(rootProject.group.toString(), name, rootProject.version.toString())

    pom {
        name = "SkyRouteMQ"
        description = "A lightweight and reliable MQTT connection manager for Android, " +
            "designed to handle long-running and persistent connections with minimal overhead"
        inceptionYear = "2025"
        url = "https://github.com/skyroute/skyroute-mq"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "andresuryana"
                name = "Andre Suryana"
                url = "https://github.com/andresuryana"
            }
        }
        scm {
            url = "https://github.com/skyroute/skyroute-mq/"
            connection = "scm:git:git://github.com/skyroute/skyroute-mq.git"
            developerConnection = "scm:git:ssh://git@github.com/skyroute/skyroute-mq.git"
        }
    }
}
