package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod

enum class TireType(
    name: String,
    val physics: Physics = Physics(),
    val appearance: Appearance = Appearance(),
    val rollingSound: ResourceLocation? = null,
    val skidSound: ResourceLocation? = null,
    val impactSound: ResourceLocation? = null,
    val skidSmoke: Boolean = true,
    val stepAssist: Boolean = true,
    val driven: Boolean = true,
    val floating: Boolean = false,
    val carving: Boolean = false,
    private val itemSuffix: String = name,
) {
    RUBBER("rubber", itemSuffix = ""),
    HONEY("honey", physics = Physics(adhesion = 500_000.0),
        appearance = Appearance(mesh = Mesh.LAYERED,
            innerTexture = ResourceLocation("minecraft", "block/honey_block_side"),
            shellTexture = ResourceLocation("minecraft", "block/honey_block_bottom"), webs = true),
        rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_honey"),
        skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_honey"),
        skidSmoke = false, stepAssist = false),
    FROST("frost", physics = Physics(staticFriction = .025, dynamicFriction = .015,
        longitudinalStiffness = 30_000.0, lateralStiffness = 30_000.0, slidingGrip = .12, slipTransitionSpeed = .08),
        appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation("minecraft", "block/blue_ice")),
        rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_icy"),
        skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_icy"),
        skidSmoke = false, stepAssist = false),
    FLOAT("float", physics = Physics(radialStiffness = 700_000.0, radialDamping = 95_000.0,
        staticFriction = .9, dynamicFriction = .7, massMultiplier = .5),
        appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation("minecraft", "block/yellow_concrete"),
            cleats = Cleats.PADDLES), floating = true),
    WOOD("wood", physics = Physics(compression = .015, radialStiffness = 40_000_000.0,
        radialDamping = 250_000.0, staticFriction = .8, dynamicFriction = .6),
        appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation("minecraft", "block/oak_planks")),
        rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_wood"), skidSmoke = false, stepAssist = false, driven = false),
    CARVER("carver", physics = Physics(compression = .06, radialStiffness = 8_000_000.0,
        radialDamping = 180_000.0, staticFriction = .6, dynamicFriction = .4, massMultiplier = 2.0),
        appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation("minecraft", "block/iron_block"),
            cleats = Cleats.TEETH), skidSmoke = false, stepAssist = false, carving = true),
    GOO("goo", physics = Physics(radialStiffness = 650_000.0, radialDamping = 2_000.0,
        treadDamping = 1_000.0, restitution = .85),
        appearance = Appearance(mesh = Mesh.LAYERED, innerTexture = ResourceLocation(ClockworkMod.MOD_ID, "block/goo"),
            shellTexture = ResourceLocation(ClockworkMod.MOD_ID, "block/gooborder")),
        rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_honey"),
        skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_honey"),
        impactSound = ResourceLocation(ClockworkMod.MOD_ID, "boing"), skidSmoke = false, stepAssist = false),
    GUMMY("gummy", physics = Physics(compression = .9, radialStiffness = 650_000.0, radialDamping = 125_000.0,
        staticFriction = 2.6, dynamicFriction = 2.1, slidingGrip = .85),
        appearance = Appearance(cleats = Cleats.LUGS));

    val id = ResourceLocation(ClockworkMod.MOD_ID, name)

    fun itemId(size: WheelSize) = if (itemSuffix.isEmpty()) size.id else size.id.replace("_wheel", "_${itemSuffix}_wheel")

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
        fun byId(id: ResourceLocation?) = entries.firstOrNull { it.id == id } ?: RUBBER
        fun read(tag: CompoundTag): TireType = if (tag.contains("TireType"))
            byId(ResourceLocation.tryParse(tag.getString("TireType")))
        else if (tag.getBoolean("HoneyWheel")) HONEY else RUBBER
    }
}
