package org.valkyrienskies.clockwork

import org.valkyrienskies.clockwork.util.gui.DuctUnits
import org.valkyrienskies.clockwork.util.kelvin.KelvinSolverType
import org.valkyrienskies.core.internal.config.ConfigCategory
import org.valkyrienskies.core.internal.config.ConfigEntry

object ClockworkConfig {
    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()

    @JvmField
    val KELVIN = Kelvin()


    class Client {
        @ConfigEntry(description = "Enable debug rendering")
        var debugRender = false

        @ConfigEntry(description = "Enable rendering particles for DuctBlock")
        var renderDuctParticles = true

        @ConfigEntry(description = "Use metric prefixes for units (15000 Pa = 15 kPa)")
        var simplifyDisplayUnits = true

        @ConfigEntry(description = "Gas mass display unit")
        var massDisplayUnit = DuctUnits.MassUnit.KILOGRAM

        @ConfigEntry(description = "Duct volume display unit")
        var volumeDisplayUnit = DuctUnits.VolumeUnit.CUBIC_METER

        @ConfigEntry(description = "Temperature display unit")
        var tempDisplayUnit = DuctUnits.TemperatureUnit.KELVIN

        @ConfigEntry(description = "Pressure display unit")
        var pressureDisplayUnit = DuctUnits.PressureUnit.PASCAL

        @ConfigEntry(description = "Gas energy display unit")
        var energyDisplayUnit = DuctUnits.EnergyUnit.JOULE

        @ConfigEntry(description = "Threshold for high temperature warning. 0.9 = 90% of maximum", min = 0.0, max = 1.0)
        var maxTemperatureWarning = 0.9

        @ConfigEntry(description = "Threshold for high pressure warning. 0.9 = 90% of maximum", min = 0.0, max = 1.0)
        var maxPressureWarning = 0.9
    }

    class Server {

        @ConfigCategory(title = "Kelvin")
        val kelvin = Kelvin()

        @ConfigEntry(description = "Enable verbose debug logging")
        var debugMode = false

        // Blacklist of blocks that don't get added for ship building
        // @ConfigEntry(description = "Blacklist of blocks that don't get assembled")
        // todo: VS config system does not support collections!
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
            "minecraft:flowing_lava",
            "vs_clockwork:physics_infuser"
        )

        @ConfigEntry(description = "Enable collision sound effects")
        var collisionSoundEffects = false

        @ConfigEntry(description = "Duct-like volume. The volume used for ducts, valves, exhausts, etc.")
        var ductVolme = 0.4

        @ConfigEntry(description = "Max collision events per tick. Dumps collision event queue if amount is bigger. Default is 100", min = 1.0)
        var collisionSoundEffectMax = 100

        @ConfigEntry(description = "Max Gravitron mass in 1000 kg")
        var maxGravitronMass = 256

        @ConfigEntry(description = "Force multiplier for balloons. Realism is 1.0, default is 1000.0. Range: > 0.0", min = 0.0)
        var balloonForceMult: Double = 50.0

        @ConfigEntry(description = "Speed multiplier for the gas nozzle pointer, default is 0.5. Range: > 0.0", min = 0.0)
        var gasNozzleSensitivity = 0.5

        @ConfigEntry(description = "Sets the gas retention efficiency of the balloon material; lower values simulate airtight rubber/synthetic, while higher values represent porous fabrics. Default 0.001.", min = 0.0, max = 1.0)
        var permeabilityConstant = 0.01

        @ConfigEntry(description = "Controls how fast air pocket temperature equalizes with the ambient temperature; lower values simulate thick insulation, while higher values cause rapid cooling or heating. Default 0.001", min = 0.0, max = 1.0)
        var heatTransferCoefficient = 0.01

        @ConfigEntry(description = "Effectiveness scalar for reaction wheels. Higher value means a single reaction wheel can better control an entire ship, regardless of its mass. Default value is 0.1.", min = 0.001, max = 1.0)
        var reactionWheelEffectiveness = 1.0

        @ConfigEntry(description = "Whether or not blade controllers consume the durability of the blades inside while rotating at high speeds.")
        var bladeControllerUsesDurability = false

        @ConfigEntry(description = "The max size that a propeller blade can reach. Sizes higher than this will refuse to craft.")
        var maxBladeSize = 8.0

        @ConfigEntry(description = "The maximum distance (in blocks) allowed between two Universal Joints while connected.", min = 1.0)
        var maxUniversalJointDistance = 10.0

        @ConfigEntry(description = "The length of the raycast made by the Gas Nozzle when attempting to find a valid balloon ceiling.", min = 1.0)
        var hotAirBalloonMaxRaycastDistance = 64.0

        @ConfigEntry(description = "The maximum volume (in blocks) that the hot air balloon floodfill will scan when trying to determine the balloon's interior.", min = 1.0)
        var hotAirBalloonMaxScanVolume = 100000.0

        @ConfigEntry(description = "The maximum surface area (in blocks) that the hot air balloon floodfill will scan when trying to determine the balloon's exterior.", min = 1.0)
        var hotAirBalloonMaxScanSurface = 100000.0

        @ConfigEntry(description = "Force multiplier when no rpm is given")
        var unlockedModeRotationResistanceMultiplier = 1.0

        @ConfigEntry()
        var unlockedModeOmegaErrorMultiplier = 50.0

        @ConfigEntry(min = 0.0, description = "Maximum torque magnitude applied by unlocked PhysBearing controller. Set 0 to disable clamping.")
        var unlockedModeMaxTorque = 100000.0

        @ConfigEntry(min = 0.0, description = "Minimum angular acceleration target for unlocked PhysBearing mode. Used to raise torque cap for heavy shiptraptions.")
        var unlockedModeMinAngularAcceleration = 50.0

        @ConfigEntry(min = 0.0, description = "Proportional gain for follow-angle PhysBearing mode.")
        var angleFollowingAngleErrorMultiplier = 60.0

        @ConfigEntry(min = 0.0, description = "Derivative gain for follow-angle PhysBearing mode.")
        var angleFollowingOmegaErrorMultiplier = 16.0

        @ConfigEntry(min = 0.0, description = "Maximum torque magnitude applied by follow-angle PhysBearing controller. Set 0 to disable clamping.")
        var angleFollowingMaxTorque = 5000.0

        @ConfigEntry(min = 0.0, description = "Maximum change in follow-angle PhysBearing torque per physics tick. Lower values reduce violent impulses.")
        var angleFollowingMaxTorqueStep = 250.0

        @ConfigEntry(min = 0.0, description = "Follow-angle deadband in degrees. Within this error band, the angle term is ignored to avoid oscillation.")
        var angleFollowingAngleDeadbandDeg = 0.75

        @ConfigEntry(min = 0.0, description = "Seconds to block Phys Bearing rotation updates after restore/reload so joints can settle. 0 disables the settle gate.")
        var physBearingRestoreSettleSeconds = 1.0

        @ConfigEntry()
        var allowWrenchingActivatedPhysBearing = false

        @ConfigEntry(min = 0.0)
        var forceMulPerSailInPropeller = 12.0

        @ConfigEntry(min = 0.0, description = "Maximum net force magnitude applied by each propeller controller update. Set 0 to disable clamping.")
        var propellerMaxForce = 200000.0

        @ConfigEntry(min = 0.0, description = "Maximum net torque magnitude applied by each propeller controller update. Set 0 to disable clamping.")
        var propellerMaxTorque = 200000.0

        @ConfigEntry(min = 0.0, description = "Damping used by extendon distance joints. Default matches legacy extendon behavior.")
        var extendonDistanceJointDamping = 1000.0

        @ConfigEntry(min = 0.0)
        var encasedFanForceMul = 40.0

        @ConfigEntry(min = 0.0, description = "Wanderlite blocks exert enough force to lift their own weight, times this multiplier.")
        var wanderOreForceMultiplier = 2.0

        @ConfigEntry(min = 0.0)
        var gasThrusterForceMul = 200.0

        @ConfigEntry(min = 0.0)
        var sugarRocketBlockThrust = 10000.0



        @ConfigEntry(description = "The amount of air (in kg) that the air compressor produces per tick at sea level and 1 rpm")
        var airCompressorSpeed = 0.0001

        @ConfigEntry(description = "The max amount of pressure that the air compressor will generate air for. In Pa")
        var airCompressorMaxPressure = 1000000.0

        @ConfigEntry(description = "Air density at which the air compressor will start generating aether. Setting it to 0 or a negative number will disable helium generation")
        var airCompressorHeliumAirDensity = 0.3

        @ConfigEntry(description = "Temperature for the gas heater to act like a passive heat source (campfires, dormant blaze burners). Default is 500K (baking oven)")
        var heaterSmoulderingTemp = 500

        @ConfigEntry(description = "Temperature for the gas heater to act like a heated blaze burner. Default is 1000K (ceramic firing)")
        var heaterKindledTemp = 1000

        @ConfigEntry(description = "Temperature for the gas heater to act like a superheated blaze burner. Default is 1500K (real metallurgy)")
        var heaterSeethingTemp = 1500

        @ConfigEntry(description = "Temperature for gas exhaust to trigger bulk smoking. Default is 500K (baking oven)")
        var bulkSmokingTemp = 500

        @ConfigEntry(description = "Temperature for gas exhaust to trigger bulk blasting. Default is 1000K (ceramic firing)")
        var bulkBlastingTemp = 1000

        @ConfigEntry(description = "Multiplier applied to ship mass when yeeting (left-clicking) with the survival gravitron", min = 0.0, max = 10000.0)
        var survivalGravitronYeetForce = 1000.0

        @ConfigEntry(description = "Maximum range (in blocks) the survival gravitron will interact with ships", min = 1.0, max = 1000.0)
        var survivalGravitronMaxRange = 20.0

        @ConfigEntry(description = "The density mult of wanderlite ore in meteors", min = 0.0)
        val meteor_density = 0.0

        @ConfigEntry(description = "Maximum amount of blocks a flap bearing can assemble", min = 0.0, max = Int.MAX_VALUE.toDouble())
        var flapBearingMaxSize = 16

        @ConfigEntry(description = "Maximum amount of blocks a smart flap bearing can assemble", min = 0.0, max = Int.MAX_VALUE.toDouble())
        var smartFlapBearingMaxSize = 24



        @ConfigEntry(description = "Whether the (smart) flap bearing peripheral can use setAngle without rotational power")
        var cheatFlapBearingPeripheral = false


    }

    class Kelvin {
        @ConfigEntry(description = "The gas physics solver used by Kelvin.")
        var kelvinSolver: KelvinSolverType = KelvinSolverType.JACOBI_SEIDEL

        @ConfigEntry(description = "Kelvin sub steps (per Tick)")
        var kelvinSubSteps = 10

        @ConfigEntry(description = "The lazytick rate for Kelvin node block entity updates")
        var kelvinNodeBlockEntityLazyTickRate = 10

        @ConfigEntry(description = "Whether the generic kelvin peripheral can move gas/heat through only a peripheral connection")
        var cheatKelvinPeripheral = false
    }
}
