plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
}

dependencies {
    api(libs.jackson.databind)
    testImplementation(libs.kotlin.test)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.named("assemble") {
    dependsOn("ktlintFormat")
}

tasks.withType<Test> {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
