plugins {
    kotlin("jvm") version "2.4.10" apply false
    alias(libs.plugins.ktlint)
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
