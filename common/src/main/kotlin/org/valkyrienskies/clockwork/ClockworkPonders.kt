package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.collision.Matrix3d
import com.simibubi.create.foundation.collision.OrientedBB
import com.tterrag.registrate.util.entry.ItemProviderEntry
import net.createmod.ponder.Ponder
import net.createmod.ponder.api.ParticleEmitter
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.EffectInstructions
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.OverlayInstructions
import net.createmod.ponder.foundation.PonderSceneBuilder
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.content.ponders.KineticPonders.redstoneResistor
import org.valkyrienskies.clockwork.content.ponders.OtherPonders.solid_delivery
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.altMeter
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.camberedWings
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.createShip
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.flap
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.gyro
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.planeTips
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.smart_flap
import org.valkyrienskies.clockwork.content.ponders.PhysicsPonders.wings

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
        HELPER.forComponents(ClockworkBlocks.ANDESITE_FLAP_BEARING, ClockworkBlocks.SMART_FLAP_BEARING)
            .addStoryBoard(
                "flap_bearing", ::flap
            )
            .addStoryBoard(
                "smart_flap_bearing", ::smart_flap
            )
            .addStoryBoard(
                "plane_tips", ::planeTips
            )
        HELPER.forComponents(ClockworkBlocks.FLAP, ClockworkBlocks.WING)
            .addStoryBoard(
                "wings", ::wings
            )
            .addStoryBoard(
                "cambered_wings",
                ::camberedWings
            )
            .addStoryBoard(
                "plane_tips", ::planeTips
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


/**
 * This function is entirely vibe coded, no idea whats going on.
 * It doesn't work perfectly, seems the origin gets misaligned sometimes.
 * But you can just compensate with a different origin, so it works good enough:tm:
 */
fun OverlayInstructions.chaseParallelogram(color: PonderPalette, origin: Vec3, width: Double, height: Double, angleRad: Double, face: Direction, duration: Int) {
    // 2D rectangle vectors
    val (wX, wY) = rotate2D(width, 0.0, angleRad)
    val (hX, hY) = rotate2D(0.0, height, angleRad)

    // Map 2D -> 3D based on face
    fun map(dx: Double, dy: Double): Vec3 = when (face) {
        Direction.UP, Direction.DOWN ->
            Vec3(origin.x + dx, origin.y, origin.z + dy)

        Direction.NORTH, Direction.SOUTH ->
            Vec3(origin.x + dx, origin.y + dy, origin.z)

        Direction.EAST, Direction.WEST ->
            Vec3(origin.x, origin.y + dy, origin.z + dx)
    }

    val bl = origin
    val br = map(wX, wY)
    val tl = map(hX, hY)
    val tr = tl.add(br.subtract(bl))

    this.showLine(color, bl, br, duration)
    this.showLine(color, br, tr, duration)
    this.showLine(color, tr, tl, duration)
    this.showLine(color, tl, bl, duration)
}

// Vibe-util
private fun rotate2D(x: Double, y: Double, angle: Double): Pair<Double, Double> {
    val cos = Math.cos(angle)
    val sin = Math.sin(angle)
    return Pair(
        x * cos - y * sin,
        x * sin + y * cos
    )
}
