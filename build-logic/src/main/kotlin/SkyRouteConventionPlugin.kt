import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class SkyRouteConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("com.diffplug.spotless")

        val licenseHeaderJava = project.rootProject.file("config/license/apache-header-java.txt")
        val licenseHeaderXml = project.rootProject.file("config/license/apache-header-xml.txt")
        val editorConfig = project.rootProject.file(".editorconfig")

        project.extensions.configure(SpotlessExtension::class.java) {
            kotlin {
                target("**/*.kt")
                targetExclude("**/build/")
                ktlint("0.50.0")
                    .setEditorConfigPath(editorConfig)
                licenseHeaderFile(licenseHeaderJava)
            }

            java {
                target("**/*.java")
                targetExclude("**/build/")
                licenseHeaderFile(licenseHeaderJava)
            }

            format("xml") {
                target("**/*.xml")
                targetExclude("**/build/**/*.xml")
                licenseHeaderFile(licenseHeaderXml, "(<[^!?])")
            }
        }
    }
}
