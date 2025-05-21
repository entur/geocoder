plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.0")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("2.1.20")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "no.entur.netex_to_json.AppKt"
}
