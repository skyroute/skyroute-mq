import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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
