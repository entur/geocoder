plugins {
    kotlin("jvm")
}

dependencies {
    api(libs.jackson.databind)
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
