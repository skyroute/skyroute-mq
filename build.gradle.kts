import com.diffplug.gradle.spotless.SpotlessExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.spotless) apply true
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/")
            ktlint("0.50.0")
                .setEditorConfigPath(rootProject.file(".editorconfig"))
            licenseHeaderFile(rootProject.file("config/license/apache-header-java.txt"))
        }

        java {
            target("**/*.java")
            targetExclude("**/build/")
            licenseHeaderFile(rootProject.file("config/license/apache-header-java.txt"))
        }

        format("xml") {
            target("**/*.xml")
            targetExclude("**/build/**/*.xml")
            licenseHeaderFile(rootProject.file("config/license/apache-header-xml.txt"), "(<[^!?])")
        }
    }
}
