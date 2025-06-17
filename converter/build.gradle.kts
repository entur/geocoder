plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.ktlint)
}

application {
    mainClass = "no.entur.netexphoton.converter.CommandKt"
}

dependencies {
    implementation(project(":common"))
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    implementation(libs.geotools.referencing)
    implementation(libs.geotools.main)
    implementation(libs.geotools.epsg.wkt)
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
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
