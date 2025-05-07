plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.3")
}

gradlePlugin {
    plugins {
        register("SkyRouteSpotless") {
            id = "skyroute-spotless"
            implementationClass = "SpotlessConventionPlugin"
        }
    }
}
