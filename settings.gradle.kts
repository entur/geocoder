plugins {
    id("io.github.ben-manes.versions.settings") version "0.59.0"
}
rootProject.name = "geocoder"
include("proxy")

dependencyResolutionManagement {
    repositories {
        mavenCentral {
            content {
                excludeGroup("javax.media") // geotools requires jai_core, which is missing from mavenCentral
            }
        }
        maven {
            url = uri("https://repo.osgeo.org/repository/geotools-releases/")
        }
    }
}
