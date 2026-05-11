pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PhantmMessenger"

include(":shared")
include(":relay")
include(":integration-tests")

// androidApp requires the Android SDK and AGP from Google Maven.
// Include it only when ANDROID_HOME is set (full Android build / CI).
if (System.getenv("ANDROID_HOME")?.isNotBlank() == true) {
    include(":androidApp")
}
