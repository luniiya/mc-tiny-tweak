pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

rootProject.name = "mc-tiny-tweak"

stonecutter {
    create(rootProject) {
        versions("1.21.11")
        vcsVersion = "1.21.11"
    }
}
