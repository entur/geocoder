plugins {
    kotlin("jvm")
    alias(libs.plugins.versions)
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-lang3") {
            version { require("[3.19.0,)") }
            because("require at least 3.19.0 to fix CVE-2025-48924")
        }
    }

    implementation(libs.geotools.referencing)
    implementation(libs.geotools.main)
    implementation(libs.geotools.epsg.wkt)
    implementation(libs.country.boundaries)

    api(libs.jackson.kotlin)
    api(libs.jackson.databind)
    api(libs.jackson.xml)
    api(libs.jackson.datatype.jsr310)

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
