plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)
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

tasks.named<Test>("test") {
    useJUnit()
    // Add :shared jvmTest and :relay test compiled class directories so that
    // AllModuleUnitTestsVerificationTest can locate test class files via Class.forName().
    dependsOn(":shared:jvmTestClasses", ":relay:testClasses")
    doFirst {
        classpath = classpath +
            project(":shared").tasks.getByName("compileTestKotlinJvm").outputs.files +
            project(":relay").tasks.getByName("compileTestKotlin").outputs.files
    }
}
