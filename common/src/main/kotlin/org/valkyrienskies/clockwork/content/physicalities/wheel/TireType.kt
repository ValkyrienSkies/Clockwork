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
    val skidSmoke: Boolean = true,
    val stepAssist: Boolean = true,
    private val itemSuffix: String = name,
) {
    RUBBER("rubber", itemSuffix = ""),
    HONEY("honey", physics = Physics(adhesion = 500_000.0),
        appearance = Appearance(mesh = Mesh.LAYERED,
            innerTexture = ResourceLocation("minecraft", "block/honey_block_side"),
            shellTexture = ResourceLocation("minecraft", "block/honey_block_bottom"), webs = true),
        rollingSound = ResourceLocation(ClockworkMod.MOD_ID, "driving_honey"),
        skidSound = ResourceLocation(ClockworkMod.MOD_ID, "tire_skid_honey"),
        skidSmoke = false, stepAssist = false);

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

    data class Appearance(
        val mesh: Mesh = Mesh.TREAD,
        val treadTexture: ResourceLocation = ResourceLocation(ClockworkMod.MOD_ID, "textures/block/wheel/tire.png"),
        val innerTexture: ResourceLocation? = null,
        val shellTexture: ResourceLocation? = null,
        val webs: Boolean = false,
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
