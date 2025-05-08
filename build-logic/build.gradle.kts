plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.spotless)
    implementation(libs.maven.publish)
}

gradlePlugin {
    plugins {
        create("skyrouteSpotless") {
            id = "skyroute-spotless"
            implementationClass = "SkyRouteConventionPlugin"
        }
        create("skyrouteMavenPublish") {
            id = "skyroute-maven-publish"
            implementationClass = "SkyRouteMavenPlugin"
        }
    }
}
