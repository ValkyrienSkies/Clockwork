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
    require(kelvin.resolve("settings.gradle.kts").isFile || kelvin.resolve("settings.gradle").isFile) {
        "Initialize the Kelvin submodule before building with includeKelvin=true."
    }
    includeBuild(kelvin.canonicalFile) {
        name = "kelvin"
        dependencySubstitution {
            for (platform in listOf("common", "fabric", "forge")) {
                val kelvinModule = "org.valkyrienskies.kelvin:kelvin-$platform"
                substitute(module(kelvinModule)).using(project(":$platform"))
            }
        }
    }
}

rootProject.name = "vs-clockwork-mod"
