plugins {
    kotlin("jvm") version "2.2.21" apply false
    alias(libs.plugins.ktlint)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        ignoreFailures.set(true)
    }
}
