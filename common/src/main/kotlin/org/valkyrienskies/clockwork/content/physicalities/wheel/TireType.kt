package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.api.wheel.TireTypes
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod

class TireType @JvmOverloads constructor(
    val id: ResourceLocation,
    val physics: Physics = Physics(),
    val appearance: Appearance = Appearance(),
    val rollingSound: ResourceLocation? = null,
    val skidSound: ResourceLocation? = null,
    val impactSound: ResourceLocation? = null,
    val skidSmoke: Boolean = true,
    private val raycastAssist: Boolean = true,
    val driven: Boolean = true,
    val floating: Boolean = false,
    val carving: Boolean = false,
    val maintenance: Maintenance = Maintenance(),
) {
    data class Maintenance(
        val wearRate: Double = 1.0,
        val punctureResistance: Double = 1.0,
        val patchMaterial: ResourceLocation? = null,
        val repairsTread: Boolean = false,
    ) {
        init {
            require(wearRate.isFinite() && wearRate >= 0)
            require(punctureResistance > 0)
            require(!repairsTread || patchMaterial != null)
        }

        val canPuncture get() = punctureResistance.isFinite()
    }

    fun stepAssist(config: ClockworkConfig.Wheel): StepAssist = when {
        !config.wheelStepAssist -> StepAssist.NONE
        config.usesRoundedContact(id.toString()) -> StepAssist.ROUNDED_CONTACT
        raycastAssist -> StepAssist.RAYCAST
        else -> StepAssist.NONE
    }

    data class Physics(
        val compression: Double = .8,
        val radialStiffness: Double = 1_000_000.0,
        val radialDamping: Double = 100_000.0,
        val longitudinalStiffness: Double = 600_000.0,
        val lateralStiffness: Double = 900_000.0,
        val treadDamping: Double = 5_000.0,
        val slidingGrip: Double = .75,
        val slipTransitionSpeed: Double = 1.5,
        val adhesion: Double = 0.0,
        val massMultiplier: Double = 1.0,
        val staticFriction: Double = 1.8,
        val dynamicFriction: Double = 1.4,
        val restitution: Double = 0.0,
    ) {
        init {
            require(compression.isFinite() && compression > 0 && compression < 1)
            require(listOf(radialStiffness, longitudinalStiffness, lateralStiffness, slipTransitionSpeed, massMultiplier)
                .all { it.isFinite() && it > 0 })
            require(listOf(radialDamping, treadDamping, adhesion, staticFriction, dynamicFriction)
                .all { it.isFinite() && it >= 0 })
            require(slidingGrip.isFinite() && slidingGrip in 0.0..1.0)
            require(restitution.isFinite() && restitution in 0.0..1.0)
        }
    }

    enum class StepAssist { NONE, RAYCAST, ROUNDED_CONTACT }
    enum class Mesh { TREAD, LAYERED }
    enum class Cleats { NONE, PADDLES, TEETH, LUGS }

    data class Appearance(
        val mesh: Mesh = Mesh.TREAD,
        val treadTexture: ResourceLocation = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/tire.png"),
        val innerTexture: ResourceLocation? = null,
        val shellTexture: ResourceLocation? = null,
        val webs: Boolean = false,
        val cleats: Cleats = Cleats.NONE,
    ) {
        init {
            require(mesh != Mesh.LAYERED || innerTexture != null)
            require(!webs || mesh == Mesh.LAYERED)
        }
    }

    companion object {
        private val SOLID = Maintenance(0.0, Double.POSITIVE_INFINITY)
        val RUBBER = TireType(ResourceLocation(ClockworkMod.MOD_ID, "rubber"))
        val HONEY = TireType(ResourceLocation(ClockworkMod.MOD_ID, "honey"), physics = Physics(adhesion = 500_000.0),
            appearance = Appearance(mesh = Mesh.LAYERED,
                innerTexture = ResourceLocation("minecraft", "block/honey_block_side"),
                shellTexture = ResourceLocation("minecraft", "block/honey_block_bottom"), webs = true),
            rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_honey"),
            skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_honey"),
            skidSmoke = false, raycastAssist = false, maintenance = Maintenance(
                patchMaterial = ResourceLocation("minecraft", "honey_block"), repairsTread = true))
        val FROST = TireType(ResourceLocation(ClockworkMod.MOD_ID, "frost"), physics = Physics(staticFriction = .025, dynamicFriction = .015,
            longitudinalStiffness = 30_000.0, lateralStiffness = 30_000.0, slidingGrip = .12, slipTransitionSpeed = .08),
            appearance = Appearance(treadTexture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/frost_tire.png")),
            rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_icy"),
            skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_icy"),
            skidSmoke = false, raycastAssist = false, maintenance = SOLID)
        val FLOAT = TireType(ResourceLocation(ClockworkMod.MOD_ID, "float"), physics = Physics(radialStiffness = 700_000.0, radialDamping = 95_000.0,
            staticFriction = .9, dynamicFriction = .7, massMultiplier = .5),
            appearance = Appearance(treadTexture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/float_tire.png"),
                cleats = Cleats.PADDLES), floating = true, maintenance = Maintenance(punctureResistance = .5))
        val WOOD = TireType(ResourceLocation(ClockworkMod.MOD_ID, "wood"), physics = Physics(compression = .015, radialStiffness = 40_000_000.0,
            radialDamping = 250_000.0, staticFriction = .8, dynamicFriction = .6),
            appearance = Appearance(treadTexture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/wooden/oak.png"),
                innerTexture = ResourceLocation("minecraft", "block/oak_planks")),
            rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_wood"), skidSmoke = false, raycastAssist = false, driven = false, maintenance = SOLID)
        val CARVER = TireType(ResourceLocation(ClockworkMod.MOD_ID, "carver"), physics = Physics(compression = .06, radialStiffness = 8_000_000.0,
            radialDamping = 180_000.0, staticFriction = .6, dynamicFriction = .4, massMultiplier = 2.0),
            appearance = Appearance(treadTexture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/carver_tire.png"),
                cleats = Cleats.TEETH), skidSmoke = false, raycastAssist = false, carving = true, maintenance = SOLID)
        val GOO = TireType(ResourceLocation(ClockworkMod.MOD_ID, "goo"), physics = Physics(radialStiffness = 650_000.0, radialDamping = 2_000.0,
            treadDamping = 1_000.0, restitution = .85),
            appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation(ClockworkMod.MOD_ID, "block/goo"),
                shellTexture = ResourceLocation(ClockworkMod.MOD_ID, "block/gooborder")),
            rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_honey"),
            skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_honey"),
            impactSound = ResourceLocation(ClockworkMod.MOD_ID, "boing"), skidSmoke = false, raycastAssist = false,
            maintenance = Maintenance(.6, Double.POSITIVE_INFINITY, ResourceLocation(ClockworkMod.MOD_ID, "goo_block"), true))
        val GUMMY = TireType(ResourceLocation(ClockworkMod.MOD_ID, "gummy"), physics = Physics(compression = .9, radialStiffness = 650_000.0, radialDamping = 125_000.0,
            staticFriction = 2.6, dynamicFriction = 2.1, slidingGrip = .85),
            appearance = Appearance(treadTexture = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/gummy_tire.png"),
                cleats = Cleats.LUGS), maintenance = Maintenance(punctureResistance = 2.0))

        internal val builtIns = listOf(RUBBER, HONEY, FROST, FLOAT, WOOD, CARVER, GOO, GUMMY)

        fun byId(id: ResourceLocation?) = id?.let(TireTypes::get) ?: RUBBER
        fun read(tag: CompoundTag): TireType? = if (tag.getBoolean("BareWheel")) null
        else if (tag.contains("TireType"))
            byId(ResourceLocation.tryParse(tag.getString("TireType")))
        else if (tag.getBoolean("HoneyWheel")) HONEY else RUBBER
    }
}
