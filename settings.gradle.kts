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

val allPlatforms = listOf("forge", "fabric")
val explicitPlatforms = providers.gradleProperty("targetPlatform").orNull
    ?.split(",")
    ?.map { it.trim().lowercase() }
    ?.filter { it in allPlatforms }
    ?.distinct()
    ?.takeIf { it.isNotEmpty() }

val requestedTaskNames = gradle.startParameter.taskNames
val inferredPlatforms = if (explicitPlatforms == null && requestedTaskNames.isNotEmpty()) {
    val taskPlatforms = requestedTaskNames.mapNotNull { task ->
        val normalized = task.removePrefix(":")
        when {
            normalized.startsWith("forge:") -> "forge"
            normalized.startsWith("fabric:") -> "fabric"
            else -> null
        }
    }
    if (taskPlatforms.size == requestedTaskNames.size) taskPlatforms.distinct() else null
} else {
    null
}

val selectedPlatforms = explicitPlatforms ?: inferredPlatforms ?: allPlatforms

include("common")
for (platform in selectedPlatforms) {
    include(platform)
}

val kelvinTasksRequested = requestedTaskNames.any { it.removePrefix(":").startsWith("kelvin:") }
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
