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
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events("failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}