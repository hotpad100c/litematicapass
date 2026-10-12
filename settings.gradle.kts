pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.8.3"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions(
            "1.21.1",
            "1.20.6",
            "1.20.4",
            "1.20.2",
            "1.20.1",
            "1.20"
        ).buildscript("build.gradle.kts")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "litematicapass"
