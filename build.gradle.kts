// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
}

group = "io.github.skyroute"
version = "0.1.0-alpha.1"

// Workaround for gradle build error "Unable to make progress running work. There are items queued for execution but none of them can be started"
// See: https://github.com/facebook/react-native/issues/44501#issuecomment-2660624072
gradle.startParameter.excludedTaskNames.addAll(
    gradle.startParameter.taskNames.filter { it.contains("testClasses") }
)
