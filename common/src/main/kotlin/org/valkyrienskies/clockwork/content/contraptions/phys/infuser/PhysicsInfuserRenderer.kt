package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.primitives.AABBi
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItem
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData

class PhysicsInfuserRenderer(context: BlockEntityRendererProvider.Context?) :
    SmartBlockEntityRenderer<PhysicsInfuserBlockEntity>(context) {
    private var teForScanner: PhysicsInfuserBlockEntity? = null
    protected override fun renderSafe(
        te: PhysicsInfuserBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
        if (te !is PhysicsInfuserBlockEntity) return
        this.teForScanner = te
        val infuser = te
        val blockState = te.blockState

        // Core
        val angle = 0f
        val offset = 0f
        if (infuser.animationType != null) {
            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                val value = infuser.assemblyProgress.value
                val coreOffset = te.getCoreOffset(partialTicks - 1)

                val crystal_inner_buffer = buffer.getBuffer(RenderType.endPortal())
                val crystal_inner = CachedBufferer.partial(ClockworkPartials.CRYSTAL_INNER, blockState)

                animateAssembly2(crystal_inner, angle, coreOffset, value, infuser).light(light).color(255,255,255, 255).overlay().disableDiffuse().renderInto(ms, crystal_inner_buffer)
                val crystal_buffer = buffer.getBuffer(ClockworkRenderTypes.CRYSTAL.apply(RenderUtil.CRYSTAL_MATRIX))
                val crystal = CachedBufferer.partial(ClockworkPartials.CRYSTAL, blockState)
                animateAssembly(crystal, angle, coreOffset, value, infuser).light(light).color(255,255,255, 255).overlay().disableDiffuse().renderInto(ms, crystal_buffer)

                val crystal_outer_buffer = buffer.getBuffer(RenderType.entityTranslucent(RenderUtil.PURPLE_HUE))
                val crystal_outer = CachedBufferer.partial(ClockworkPartials.CRYSTAL_OUTER, blockState)
                animateAssembly(crystal_outer, angle, coreOffset, value, infuser).light(light).color(255,255,255, 255).overlay().renderInto(ms, crystal_outer_buffer)
            }

            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.IDLE) {
                val crystal_inner_buffer = buffer.getBuffer(RenderType.endPortal())
                val crystal_inner = CachedBufferer.partial(ClockworkPartials.CRYSTAL_INNER, blockState)

                idleRotateCore(crystal_inner, angle, offset, infuser).light(light).color(255,255,255, 255).overlay().disableDiffuse().renderInto(ms, crystal_inner_buffer)
                val crystal_buffer = buffer.getBuffer(ClockworkRenderTypes.CRYSTAL.apply(RenderUtil.CRYSTAL_MATRIX))
                val crystal = CachedBufferer.partial(ClockworkPartials.CRYSTAL, blockState)
                idleRotateCore(crystal, angle, offset, infuser).light(light).color(255,255,255, 255).overlay().disableDiffuse().renderInto(ms, crystal_buffer)

                val crystal_outer_buffer = buffer.getBuffer(RenderType.entityTranslucent(RenderUtil.PURPLE_HUE))
                val crystal_outer = CachedBufferer.partial(ClockworkPartials.CRYSTAL_OUTER, blockState)
                idleRotateCore(crystal_outer, angle, offset, infuser).light(light).color(255,255,255, 255).overlay().renderInto(ms, crystal_outer_buffer)
            }
        }
    }

    private fun idleRotateCore(
        buffer: SuperByteBuffer,
        angle: Float,
        offset: Float,
        infuser: PhysicsInfuserBlockEntity
    ): SuperByteBuffer {
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        val scale = 1.5f
        buffer.scale(scale)
        buffer.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))
        buffer.rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat()).translate(0.0, offset.toDouble(), 0.0)
        buffer.translateY(-(4.5 / 16.0))

        return buffer
    }


    private fun animateAssembly(
        buffer: SuperByteBuffer,
        angle: Float,
        coreOffset: Float,
        value: Float,
        infuser: PhysicsInfuserBlockEntity
    ): SuperByteBuffer {
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        val scale = 1.5f
        buffer.scale(scale)
        buffer.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))

        buffer.translateY((coreOffset * 2).toDouble()).rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
        buffer.rotateCentered(Direction.NORTH, (interpolatedAngle / 180 * Math.PI).toFloat())
        buffer.translateY(-(4.5 / 16.0))
        return buffer
    }

    private fun animateAssembly2(
        buffer: SuperByteBuffer,
        angle: Float,
        coreOffset: Float,
        value: Float,
        infuser: PhysicsInfuserBlockEntity
    ): SuperByteBuffer {
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        val scale = 1.5f
        buffer.scale(scale)
        buffer.translate(-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)),-(1 / (scale.toDouble() * 4)))

        buffer.translateY((coreOffset * 2).toDouble()).rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
        buffer.rotateCentered(Direction.NORTH, (interpolatedAngle / 180 * Math.PI).toFloat())
        buffer.translateY(-(4.5 / 16.0))
        return buffer
    }

    @Environment(EnvType.CLIENT)
    inner class ScanManager {
        // --------------------------------------------------------------------- //
        fun computeScanGrowthDuration(): Int {
            return teForScanner!!.scanGrowthDuration
        }


    }

    companion object {
        // The number of ticks over which to compute scan results. Which is at the
        // same time the use time of the scanner item.
        const val SCAN_COMPUTE_DURATION = 40

        // Initial radius of the scan wave.
        const val SCAN_INITIAL_RADIUS = 10

        // Scan wave growth time offset to avoid super slow start speed.
        const val SCAN_TIME_OFFSET = 200

        // How long the ping takes to reach the end of the visible area.
        const val SCAN_GROWTH_DURATION = 2000

        // Reference render distance the above constants are relative to.
        private const val REFERENCE_RENDER_DISTANCE = 12

        // --------------------------------------------------------------------- //
        var scanningTicks = -1

        // --------------------------------------------------------------------- //
        // List of providers currently used to scan.
        var currentStart: Long = -1
        var lastScanCenter: Vec3? = null
        var viewModelStack: PoseStack? = null
        var projectionMatrix: Matrix4f? = null

        // --------------------------------------------------------------------- //
        fun cancelScan() {
            scanningTicks = 0
        }

        private fun clear() {
            lastScanCenter = null
            currentStart = -1
        }
    }
}