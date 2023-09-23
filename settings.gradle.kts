pluginManagement {
    repositories {
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
        maven("https://maven.quiltmc.org/repository/release") {
            name = "Quilt"
        }
    }

    resolutionStrategy {
        eachPlugin {
            // If we request Forge, actually give it the correct artifact.
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }

            if (requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true) {
                val kotlin_version: String by settings
                useVersion(kotlin_version)
            }
        }
    }
}

include("common")
include("fabric")
include("forge")

rootProject.name = "vs-clockwork-mod"
