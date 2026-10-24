pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "aeronautics-bundle-fabric-26.3"

include("simulated:fabric", "aeronautics:fabric", "offroad:fabric")
