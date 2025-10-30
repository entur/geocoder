plugins {
    kotlin("jvm") version "2.2.21" apply false
    alias(libs.plugins.ktlint)
}

//subprojects {
//    apply(plugin = "org.jlleitschuh.gradle.ktlint")
//
//    afterEvaluate {
//        tasks.findByName("build")?.dependsOn("ktlintFormat")
//    }
//}

