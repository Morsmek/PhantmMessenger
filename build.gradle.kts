plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Android plugins are only declared when the Android SDK is present.
// In CI / Android environments, set ANDROID_HOME to enable Android targets.
val androidSdkPresent = System.getenv("ANDROID_HOME")?.isNotBlank() == true
if (androidSdkPresent) {
    apply(plugin = libs.plugins.androidApplication.get().pluginId)
    apply(plugin = libs.plugins.androidLibrary.get().pluginId)
    apply(plugin = libs.plugins.kotlinAndroid.get().pluginId)
}
