// Applied only when ANDROID_HOME is set and com.android.library plugin is active.
android {
    namespace = "com.stagic.phantm.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
