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

        @JsonSchema(description = "Force multiplier for balloons. Realism is 1.0, default is 1000.0. Range: > 0.0", min = 0.0)
        var balloonForceMult: Double = 1000.0

        @JsonSchema(description = "Leakage rate of pockets. Determines how fast pressure in an unsealed pocket attempts to 'normalize'. Default is 0.5.", min = 0.0, max = 1.0)
        var pocketLeakageRate = 0.5

        @JsonSchema(description = "Effectiveness scalar for reaction wheels. Higher value means a single reaction wheel can better control an entire ship, regardless of its mass. Default value is 0.1.", min = 0.001, max = 1.0)
        var reactionWheelEffectiveness = 1.0

        @JsonSchema()
        var lockedModeBaseAngleErrorMultiplier = 1.0

        @JsonSchema()
        var unlockedModeRotationResistanceMultiplier = 1.0
    }
}