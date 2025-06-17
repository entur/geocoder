plugins {
    kotlin("jvm") version "2.1.20"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktlint.cli)
    implementation(libs.ktlint.ruleset)
}
