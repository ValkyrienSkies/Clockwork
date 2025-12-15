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
}

include("common")
include("forge")
include("fabric")

//include("api")
//include("impl")

try {
    val kelvin = file("kelvin")
    if (kelvin.isDirectory) {
        //ts is evil as hell, but we have to do it because of an intellij bug :(
        includeBuild(kelvin.toPath().toRealPath().toAbsolutePath().toString())
    }
} catch (ignore: SecurityException) {}

rootProject.name = "vs-clockwork-mod"
