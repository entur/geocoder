plugins {
    kotlin("jvm") version "2.1.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktlint.cli)
    implementation(libs.ktlint.ruleset)
}
