plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

// Android targets require the Android SDK and AGP (com.android.library from Google Maven).
// Apply the plugin and configure Android targets only when ANDROID_HOME is set.
val androidSdkPresent = System.getenv("ANDROID_HOME")?.isNotBlank() == true
if (androidSdkPresent) {
    apply(plugin = libs.plugins.androidLibrary.get().pluginId)
}

kotlin {
    jvm()
    if (androidSdkPresent) {
        androidTarget {
            compilations.all {
                compilerOptions.configure {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val jvmAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.lazysodium.java)
                implementation(libs.jna)
                implementation(libs.bouncycastle.bcprov)
                implementation(libs.ktor.client.okhttp)
            }
        }
        jvmMain.get().dependsOn(jvmAndroidMain)

        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        if (androidSdkPresent) {
            androidMain.get().dependsOn(jvmAndroidMain)
            androidMain.dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.sqldelight.android)
                implementation(libs.sqlcipher.android)
                implementation(libs.androidx.sqlite)
            }
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        jvmTest.dependencies {
            implementation(kotlin("reflect"))
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
            implementation(libs.ktor.client.darwin)
        }
    }
}

sqldelight {
    databases {
        create("PhantmDatabase") {
            packageName.set("com.stagic.phantm.db")
        }
    }
}

if (androidSdkPresent) {
    apply(from = "android-shared.gradle.kts")
}
