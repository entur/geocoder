rootProject.name = "netex-photon"
include("common")
include("converter")
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