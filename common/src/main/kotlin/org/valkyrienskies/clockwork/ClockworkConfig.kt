package org.valkyrienskies.clockwork

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema

object ClockworkConfig {
    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()


    class Client {
        @JsonSchema(description = "Enable debug rendering")
        var debugRender = false
    }

    class Server {
        @JsonSchema(description = "Enable verbose debug logging")
        var debugMode = false

        @JsonSchema(description = "Kelvin sub steps (per Tick)")
        var kelvinSubSteps = 10
      
        // Blacklist of blocks that don't get added for ship building
        @JsonSchema(description = "Blacklist of blocks that don't get assembled")
        var blockBlacklist = setOf(
            "minecraft:bedrock",
            "minecraft:end_portal_frame",
            "minecraft:end_portal",
            "minecraft:end_gateway",
            "minecraft:portal",
            "minecraft:air",
            "minecraft:water",
            "minecraft:flowing_water",
            "minecraft:lava",
            "minecraft:flowing_lava"
        )

        @JsonSchema(description = "Max Gravitron mass in 1000 kg")
        var maxGravitronMass = 256
    }
}