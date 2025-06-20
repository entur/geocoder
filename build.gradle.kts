plugins {
    kotlin("jvm") version "2.1.21" apply false
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0-rc.1" apply false
}


subprojects {
    tasks.matching { it.name == "assemble" }.configureEach {
        dependsOn("ktlintFormat")
    }
}
