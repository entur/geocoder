rootProject.name = "netex-photon"
include("common")
include("converter")
include("proxy")

dependencyResolutionManagement {
    repositories {
        mavenCentral {
            content {
                excludeGroup("javax.media") // jai_core is missing from mavenCentral
            }
        }
        maven {
            url = uri("https://repo.osgeo.org/repository/geotools-releases/")
        }
    }
}