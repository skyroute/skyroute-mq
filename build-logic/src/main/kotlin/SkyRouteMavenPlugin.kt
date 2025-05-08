import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.Plugin
import org.gradle.api.Project

class SkyRouteMavenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.vanniktech.maven.publish")

        project.extensions.configure(MavenPublishBaseExtension::class.java) {
            publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
            signAllPublications()

            coordinates(project.rootProject.group.toString(), project.name, project.rootProject.version.toString())

            pom {
                name.set("SkyRouteMQ")
                description.set("A lightweight and reliable MQTT connection manager for Android, " +
                    "designed to handle long-running and persistent connections with minimal overhead")
                inceptionYear.set("2025")
                url.set("https://github.com/skyroute/skyroute-mq")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("andresuryana")
                        name.set("Andre Suryana")
                        url.set("https://github.com/andresuryana")
                    }
                }
                scm {
                    url.set("https://github.com/skyroute/skyroute-mq/")
                    connection.set("scm:git:git://github.com/skyroute/skyroute-mq.git")
                    developerConnection.set("scm:git:ssh://git@github.com/skyroute/skyroute-mq.git")
                }
            }
        }
    }
}
