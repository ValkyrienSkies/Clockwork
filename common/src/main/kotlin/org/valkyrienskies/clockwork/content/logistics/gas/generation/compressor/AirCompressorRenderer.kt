package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.particle.AirParticleData
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.common.util.toDoubles
import kotlin.math.abs

class AirCompressorRenderer(context: BlockEntityRendererProvider.Context?) : KineticBlockEntityRenderer<AirCompressorBlockEntity>(context) {

    companion object {
        // --- Animation Constants ---
        /** Factor to convert rotational speed to client animation size change per tick. */
        private const val SPEED_TO_SIZE_FACTOR = 1f / 1500f
        private const val BASE_FABRIC_SCALE_Y = 0.5f
        private const val TOP_TRANSLATION_FACTOR = 1.5f

        // --- Particle Constants ---
        private const val PARTICLE_BURST_COUNT = 36
        private const val PARTICLE_Y_OFFSET = 1.6f
        private const val PARTICLE_VERTICAL_SPEED = 0.05
        private const val PARTICLE_AIR_DATA_SCALE = 0.00f
        private const val PARTICLE_AIR_DATA_DRAG = 0.005f
        private const val PARTICLE_HORIZONTAL_SPREAD = 0.5f
    }

    override fun renderSafe(
        be: AirCompressorBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {

        // We recalculate light based on original code logic, ignoring the passed 'light' parameter
        val level = be.level ?: return // Ensure level exists

        val blockPos = be.blockPos
        val lightBelow = LevelRenderer.getLightColor(level, blockPos.below())
        val lightAbove = LevelRenderer.getLightColor(level, blockPos.above())

        val vb = buffer.getBuffer(RenderType.cutoutMipped())

        val axisPartial = CachedBuffers.partial(ClockworkPartials.COMPRESSOR_AXIS, be.blockState)
        standardKineticRotationTransform(axisPartial, be, lightBelow).renderInto(ms, vb)


        updateClientSize(be, partialTicks)
        handleParticleSpawning(be, level)


        val fabricPartial = CachedBuffers.partial(ClockworkPartials.COMPRESSOR_FABRIC, be.blockState)
        val topPartial = CachedBuffers.partial(ClockworkPartials.COMPRESSOR_TOP, be.blockState)

        // easedHalfSize ranges from 0.0 (fully deflated) to 0.5 (fully inflated)
        val easedHalfSize = EaseHelper.easeInOutQuad(be.clientSize) / 2.0f

        fabricPartial.apply {
            scale(1.0f, BASE_FABRIC_SCALE_Y + easedHalfSize, 1.0f)
            translate(0.0, (BASE_FABRIC_SCALE_Y - easedHalfSize) / 2.0, 0.0) // Divide by 2.0 for Float division
            light<SuperByteBuffer>(lightAbove)
        }.renderInto(ms, vb)

        topPartial.apply {
            translate(0.0, ((easedHalfSize - BASE_FABRIC_SCALE_Y) * TOP_TRANSLATION_FACTOR).toDouble(), 0.0)
            light<SuperByteBuffer>(lightAbove)
        }.renderInto(ms, vb)
    }

    /**
     * Updates the client-side animation progress (`clientSize`) based on speed and time.
     */
    private fun updateClientSize(be: AirCompressorBlockEntity, partialTicks: Float) {
        val minecraft = Minecraft.getInstance()
        // Determine direction of size change: +1 (inflating), -1 (deflating), 0 (paused)
        val sizeMultiplier = when {
            minecraft.isPaused -> 0f
            // Deflate if particles were spawned OR if the compressor is off
            be.clientParticles || !be.isActivated -> -1f
            else -> 1f
        }

        be.clientSize += partialTicks * abs(be.speed) * sizeMultiplier * SPEED_TO_SIZE_FACTOR
        be.clientSize = Mth.clamp(be.clientSize, 0f, 1f)
    }

    /**
     * Handles spawning particles when the compressor completes a cycle and resetting the particle flag.
     */
    private fun handleParticleSpawning(be: AirCompressorBlockEntity, level: Level) {
        // Spawn particle burst when fully inflated and particles haven't been spawned yet this cycle
        if (be.clientSize >= 1.0f && !be.clientParticles) {
            be.clientParticles = true
            spawnAirBurstParticles(be.blockPos, level)

            val pitch = abs(be.speed) / 256f + level.random.nextFloat() + 0.25f
            AllSoundEvents.STEAM.playAt(level, be.blockPos.toDoubles(), 0.4f, pitch, true)

        }

        // Reset particle flag when fully deflated
        if (be.clientSize <= 0.0f) {
            be.clientParticles = false
        }
    }

    /**
     * Spawns a burst of air particles around the top of the compressor.
     */
    private fun spawnAirBurstParticles(pos: BlockPos, level: Level) {
        val xBase = pos.x + 0.5
        val yBase = pos.y + PARTICLE_Y_OFFSET
        val zBase = pos.z + 0.5

        repeat(PARTICLE_BURST_COUNT) {
            // Random offsets within a horizontal radius and slightly above the block
            val rX = (level.random.nextFloat() - 0.5f) * 2 * PARTICLE_HORIZONTAL_SPREAD
            val rY = level.random.nextFloat().toDouble()
            val rZ = (level.random.nextFloat() - 0.5f) * 2 * PARTICLE_HORIZONTAL_SPREAD

            level.addParticle(
                AirParticleData(PARTICLE_AIR_DATA_SCALE, PARTICLE_AIR_DATA_DRAG),
                xBase + rX,
                yBase + rY,
                zBase + rZ,
                0.0,
                PARTICLE_VERTICAL_SPEED,
                0.0
            )
        }
    }
}
