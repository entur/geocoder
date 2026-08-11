import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.shadow)
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version = "1.8.0"
    ignoreFailures = true
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
    implementation(libs.geotools.referencing)
    implementation(libs.geotools.main)
    implementation(libs.geotools.epsg.wkt)
    implementation(libs.country.boundaries)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.xml)
    implementation(libs.jackson.datatype.jsr310)
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

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty") {
            useVersion("4.2.16.Final")
            because("force latest version to fix CVE-2026-56819")
        }
        if (requested.group.startsWith("tools.jackson")) {
            useVersion("3.1.4")
            because("force latest version to fix CVE-2026-59888")
        }
    }
}

testing {
    suites {
        named<JvmTestSuite>("test") {
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

val gitHashProvider =
    providers
        .exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText
        .map { it.trim() }

tasks.withType<ShadowJar> {
    // INCLUDE is required so Shadow's transformers see every colliding copy of a
    // file. Under the default EXCLUDE, duplicates are dropped before the transformer
    // runs - which silently breaks service merging (e.g. Jackson's KotlinModule gets
    // dropped) and floods the build with warnings.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // Merge META-INF/services so no ServiceLoader provider (GeoTools SPIs, Jackson
    // modules) is lost when the dependencies' copies collide.
    mergeServiceFiles()

    // Nothing compiles against this runnable app jar, so the compile-time-only
    // .kotlin_module metadata is dead weight - drop it from the fat jar.
    exclude("**/*.kotlin_module")

    manifest {
        attributes(mapOf("Implementation-Version" to gitHashProvider))
    }
}
