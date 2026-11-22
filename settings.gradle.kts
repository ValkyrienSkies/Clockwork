pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        maven("https://repo.spongepowered.org/repository/maven-public/") {
            name = "Sponge Snapshots"
        }
        maven("https://maven.minecraftforge.net") {
            name = "Forge"
        }
        maven("https://maven.architectury.dev/") {
            name = "Architectury"
        }
    }

    resolutionStrategy {
        eachPlugin {
            // If we request Forge, actually give it the correct artifact.
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }
    plugins {
        kotlin("kapt") version "2.0.0"
    }
}

include("common")
include("forge")
include("fabric")

val kelvinTasksRequested = gradle.startParameter.taskNames.any { it.removePrefix(":").startsWith("kelvin:") }
if (kelvinTasksRequested || providers.gradleProperty("includeKelvin").orNull.toBoolean()) {
    val kelvin = file("Kelvin")
    if (kelvin.resolve("settings.gradle.kts").isFile || kelvin.resolve("settings.gradle").isFile) {
        includeBuild(kelvin.canonicalFile) {
            name = "kelvin"
        }
    }
}

rootProject.name = "vs-clockwork-mod"
