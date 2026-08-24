import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ApacheNoticeResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.MergeLicenseResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PreserveFirstFoundResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PropertiesFileTransformer

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

    // Aggregate the dependencies' LICENSE and NOTICE files instead of shipping one colliding
    // copy per jar. These includes add to the transformer's own defaults; about.html carries
    // the EPL notices in the EMF jars. No SPDX id: the aggregate mixes several licences, and
    // the header would declare the whole file as one of them.
    transform<MergeLicenseResourceTransformer> {
        artifactLicense.set(layout.settingsDirectory.file("LICENSE.md"))
        artifactLicenseSpdxId.set("")
        include("about.html", "META-INF/*-LICENSE")
    }
    transform<ApacheNoticeResourceTransformer> {
        addHeader.set(false)
        projectName.set("Entur geocoder proxy")
        organizationName.set("Entur AS")
        organizationURL.set("https://entur.no/")
        // Without an explicit copyright the transformer stamps the current year, which is
        // not a task input - cached and fresh builds would then disagree.
        copyright.set("Entur geocoder proxy\nCopyright 2025 Entur AS\n")
    }

    // Netty's build info is module-prefixed per jar, and the three EMF plugin.properties have
    // near-disjoint keys, so merging each set loses nothing. Merging matters for the EMF one:
    // only one copy of a duplicated name is reachable, so picking a copy strands the others'
    // keys and EMFPlugin.getString then throws. paths also keeps this off other .properties.
    transform<PropertiesFileTransformer> {
        paths.addAll("META-INF/io.netty.versions.properties", """^plugin\.properties$""")
    }

    // Eclipse plugin descriptors and branding from the EMF jars GeoTools pulls in. Nothing
    // reads them outside OSGi, and only one copy was reachable in the jar anyway.
    transform<PreserveFirstFoundResourceTransformer> {
        include(
            "plugin.xml",
            "about.ini",
            "about.mappings",
            "about.properties",
            "modeling32.png",
        )
    }

    // Nothing compiles against this runnable app jar, so the compile-time-only
    // .kotlin_module metadata is dead weight - drop it from the fat jar.
    exclude("**/*.kotlin_module")

    manifest {
        attributes(mapOf("Implementation-Version" to gitHashProvider))
    }
}
