plugins {
    kotlin("jvm") version "2.3.0" apply false
    alias(libs.plugins.ktlint)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        ignoreFailures.set(true)
    }

    ktlint {
        version = "1.8.0"
    }
}
