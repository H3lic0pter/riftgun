pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        maven("https://maven.neoforged.net/releases")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    // The root project is the Tree: it stores the shared sources and build logic.
    // Each supported Minecraft version is a node subproject under versions/.
    create(rootProject) {
        versions("1.21.1", "26.1.2")
        // vcs("1.21.1") // reset point when switching the active version
    }
}

rootProject.name = "riftgun"
