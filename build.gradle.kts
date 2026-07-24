plugins {
    kotlin("jvm") version "2.4.10" apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versions)
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

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return !isStable
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.pinterest.ktlint") {
            useVersion("1.8.0")
        }
    }
}

// https://github.com/ben-manes/gradle-versions-plugin/issues/968
tasks.dependencyUpdates {
    doFirst {
        gradle.startParameter.isParallelProjectExecutionEnabled = false
    }
}
