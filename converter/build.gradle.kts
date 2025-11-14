plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass = "no.entur.geocoder.converter.cli.CommandKt"
}

dependencies {
    implementation(project(":common"))
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.xml)
    implementation(libs.jackson.datatype.jsr310)

    implementation("org.openstreetmap.osmosis:osmosis-core:0.49.2")
    implementation("org.openstreetmap.osmosis:osmosis-pbf:0.49.2")
    implementation("org.openstreetmap.osmosis:osmosis-xml:0.49.2")

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
