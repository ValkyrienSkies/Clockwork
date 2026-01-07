package org.valkyrienskies.clockwork

import com.tterrag.registrate.util.entry.ItemProviderEntry
import net.createmod.ponder.Ponder
import net.createmod.ponder.api.ParticleEmitter
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.level.PonderLevel
import net.createmod.ponder.api.scene.EffectInstructions
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.foundation.PonderSceneBuilder
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.valkyrienskies.clockwork.content.ponders.KineticPonders.redstoneResistor
import org.valkyrienskies.clockwork.content.ponders.OtherPonders.solid_delivery
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.altMeter
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.createShip
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.flap
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.gyro
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.smart_flap

object ClockworkPonders {

    fun init(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        val HELPER: PonderSceneRegistrationHelper<ItemProviderEntry<*>> = helper.withKeyFunction { it.id }
        HELPER.forComponents(ClockworkItems.WANDERWAND, ClockworkBlocks.PHYSICS_INFUSER)
            .addStoryBoard(
                "wanderwand", ::createShip
            )
        HELPER.forComponents(ClockworkBlocks.REDSTONE_RESISTOR)
            .addStoryBoard(
                "resistor", ::redstoneResistor
            )

        HELPER.forComponents(ClockworkBlocks.ALT_METER)
            .addStoryBoard(
                "alt_meter", ::altMeter
            )
        HELPER.forComponents(ClockworkBlocks.ANDESITE_FLAP_BEARING, ClockworkBlocks.SMART_FLAP_BEARING, ClockworkBlocks.FLAP)
            .addStoryBoard(
                "flap_bearing", ::flap
            )
            .addStoryBoard(
                "smart_flap_bearing", ::smart_flap
            )
        HELPER.forComponents(ClockworkBlocks.GYRO).addStoryBoard(
            "gyro", ::gyro
        )
        HELPER.forComponents(ClockworkBlocks.DELIVERY_CANNON, ClockworkBlocks.DELIVERY_CHUTE)
            .addStoryBoard(
                "solid_delivery", ::solid_delivery
            )
    }



}

fun PonderSceneBuilder.ponderLang(index: Int): String {
    return Component.translatable("${ClockworkMod.MOD_ID}.ponder.${this.scene.id.path}.text_$index").string
}

fun PonderSceneBuilder.sparklingPlane(area: AABB, color: PonderPalette, direction: Direction, duration: Int) {
    val plane = when (direction) {
        // Flatten the Y to the bottom
        Direction.UP -> AABB(area.minX, area.minY, area.minZ, area.maxX, area.minY, area.maxZ)
        // Flatten the Y to the top
        Direction.DOWN -> AABB(area.minX, area.maxY, area.minZ, area.maxX, area.maxY, area.maxZ)
        // Flatten the X to the +
        Direction.WEST -> AABB(area.maxX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)
        // Flatten the X to the -
        Direction.EAST -> AABB(area.minX, area.minY, area.minZ, area.minX, area.maxY, area.maxZ)
        // Flatten the Z to the +
        Direction.NORTH -> AABB(area.minX, area.minY, area.maxZ, area.maxX, area.maxY, area.maxZ)
        // Flatten the Z to the -
        Direction.SOUTH -> AABB(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.minZ)
    }

    this.overlay().chaseBoundingBoxOutline(color, plane, plane, duration)

    this.effects().emitParticles(
        Vec3(plane.minX, plane.minY, plane.minZ),
        this.effects().particleEmitterWithinAABB (
            DustParticleOptions(
                Vec3.fromRGB24(color.color).toVector3f(),
                1.0F
            ),
            Vec3(0.0, 0.0, 0.0), // Dust particles don't use motion :(
            area
        ),
        10F,
        duration
    )
}

fun <T : ParticleOptions> EffectInstructions.particleEmitterWithinAABB(data: T, motion: Vec3, area: AABB): ParticleEmitter {
    return ParticleEmitter { w, x, y, z ->
        w.addParticle(
            data,
            area.minX+(Ponder.RANDOM.nextDouble()*(area.maxX-area.minX)),
            area.minY+(Ponder.RANDOM.nextDouble()*(area.maxY-area.minY)),
            area.minZ+(Ponder.RANDOM.nextDouble()*(area.maxZ-area.minZ)),
            motion.x,
            motion.y,
            motion.z
        )
    }
}