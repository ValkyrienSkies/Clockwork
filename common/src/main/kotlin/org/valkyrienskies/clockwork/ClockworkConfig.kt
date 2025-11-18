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

        @JsonSchema(description = "Enable rendering particles for DuctBlock")
        var renderDuctParticles = true
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

        @JsonSchema(description = "Enable collision sound effects")
        var collisionSoundEffects = false

        @JsonSchema(description = "Max collision events per tick. Dumps collision event queue if amount is bigger. Default is 100", min = 1.0)
        var collisionSoundEffectMax = 100

        @JsonSchema(description = "Max Gravitron mass in 1000 kg")
        var maxGravitronMass = 256

        @JsonSchema(description = "Force multiplier for balloons. Realism is 1.0, default is 1000.0. Range: > 0.0", min = 0.0)
        var balloonForceMult: Double = 1000.0

        @JsonSchema(description = "Leakage rate of pockets. Determines how fast pressure in an unsealed pocket attempts to 'normalize'. Default is 0.5.", min = 0.0, max = 1.0)
        var pocketLeakageRate = 0.5

        @JsonSchema(description = "Effectiveness scalar for reaction wheels. Higher value means a single reaction wheel can better control an entire ship, regardless of its mass. Default value is 0.1.", min = 0.001, max = 1.0)
        var reactionWheelEffectiveness = 1.0

        @JsonSchema(description = "Whether or not blade controllers consume the durability of the blades inside while rotating at high speeds.")
        var bladeControllerUsesDurability = false

        @JsonSchema(description = "The substeps of blade force calculation. More steps means more \'accurate\' simulation, but also makes it significantly more performance heavy.")
        var bladeIntegrationSteps = 10.0

        @JsonSchema(description = "Force multiplier when no rpm is given")
        var angleFollowingBaseAngleErrorMultiplier = 2.0

        @JsonSchema()
        var angleFollowingAngleErrorMultiplier = 50.0

        @JsonSchema()
        var angleFollowingOmegaErrorMultiplier = 10.0

        @JsonSchema(min = 0.0)
        var forceMulPerSailInPropeller = 500.0

        @JsonSchema(min = 0.0)
        var encasedFanForceMul = 40.0

        @JsonSchema(min = 0.0)
        var wanderOreForce = 1100.0

        @JsonSchema(min = 0.0)
        var gasThrusterForceMul = 2.0

        @JsonSchema(min = 0.0)
        var sugarRocketBlockThrust = 10000.0

        @JsonSchema(description = "The amount of air (in kg) that the air compressor produces per tick at sea level and 1 rpm")
        var airCompressorSpeed = 0.1

        @JsonSchema(description = "The max amount of pressure that the air compressor will generate air for. In Pa")
        var airCompressorMaxPressure = 1000000.0

        @JsonSchema(description = "Air density at which the air compressor will start generating helium. Setting it to 0 or even a negative number should just disable helium generation")
        var airCompressorHeliumAirDensity = 0.3
    }
}