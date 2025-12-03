plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.versions)
}

application {
    mainClass = "no.entur.geocoder.converter.cli.CommandKt"
}

dependencies {
    implementation(project(":common"))

    implementation(libs.osmosis.core)
    implementation(libs.osmosis.pbf)
    implementation(libs.osmosis.xml)

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
