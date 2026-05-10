plugins {
    kotlin("jvm") version "2.0.20"
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(project(":shared"))
    testImplementation(project(":relay"))
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlinx.serialization.protobuf)
    testImplementation(libs.sqldelight.sqlite.driver)
    testImplementation(libs.jna)
}

tasks.test {
    useJUnit()
}
