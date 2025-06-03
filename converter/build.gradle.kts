plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass = "no.entur.netex_photon.converter.CommandKt"
}

dependencies {
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.engine)
    testImplementation(libs.kotlin.test.junit5) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
