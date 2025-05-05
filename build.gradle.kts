// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "io.github.skyroute"
version = "0.1.0-alpha.1"

subprojects {
    apply(plugin = "skyroute-spotless")
}
