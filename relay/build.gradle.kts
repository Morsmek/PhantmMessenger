plugins {
    kotlin("jvm") version "2.0.20"
    application
}

application {
    mainClass.set("com.stagic.phantm.relay.MainKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.coroutines.core)
}
