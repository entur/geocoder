import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
}

application {
    mainClass.set("no.entur.geocoder.proxy.AppKt")
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-lang3") {
            version { require("[3.19.0,)") }
            because("require at least 3.19.0 to fix CVE-2025-48924")
        }
        implementation("commons-codec:commons-codec") {
            version { require("[1.20.0,)") }
            because("require at least 1.20.0 to fix CVE-2025-48924 and CVE-2020-15250")
        }
    }
    implementation(project(":common"))
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback)
    implementation(libs.logback.encoder)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.metrics)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
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

val gitHashProvider =
    providers
        .exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText
        .map { it.trim() }

tasks.withType<ShadowJar> {
    manifest {
        attributes(mapOf("Implementation-Version" to gitHashProvider))
    }
}
