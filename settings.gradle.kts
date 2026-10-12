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
            "1.21.11",
            "1.21.10",
            "1.21.8",
            "1.21.5"
        ).buildscript("build.gradle.kts")
        versions(
            "26.3",
            "26.2",
            "26.1.2"
        ).buildscript("unobfuscated.gradle.kts")
        vcsVersion = "26.3"
    }
}

rootProject.name = "litematicapass"
