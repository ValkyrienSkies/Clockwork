package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Matrix4f
import com.mojang.math.Quaternion
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials

class PhysicsInfuserRenderer(context: BlockEntityRendererProvider.Context?) :
    SmartBlockEntityRenderer<PhysicsInfuserBlockEntity>(context) {
    private var te: PhysicsInfuserBlockEntity? = null
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
        this.te = te
        val infuser = te
        val blockState = te.blockState
        val vb = buffer.getBuffer(RenderType.translucent())

        // Render Mysterious Liquid
        val mysteriousLiquid = CachedBufferer.partial(ClockworkPartials.STRANGE_FLUID, blockState)
        mysteriousLiquid.light(light).renderInto(ms, buffer.getBuffer(RenderType.translucent()))
        // Zappin'
        var zap1: SuperByteBuffer? = null
        var zap2: SuperByteBuffer? = null
        var zap3: SuperByteBuffer? = null
        if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
            zap1 = CachedBufferer.partial(ClockworkPartials.ZAP, blockState)
            zap2 = CachedBufferer.partial(ClockworkPartials.ZAP, blockState)
            zap3 = CachedBufferer.partial(ClockworkPartials.ZAP, blockState)
        } else {
            zap1 = null
            zap2 = null
            zap3 = null
        }

        // Core
        val core = CachedBufferer.partial(ClockworkPartials.PHYSICS_CORE, blockState)
        val angle = 0f
        val offset = 0f
        if (infuser.animationType != null) {
            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                val value = infuser.assemblyProgress.value
                val coreOffset = te.getCoreOffset(partialTicks - 1)
                animateAssembly(core, angle, coreOffset, value, infuser).light(light).renderInto(ms, vb)
                if (value >= 160 && value <= 200 || value >= 300 && value <= 340 || value >= 420 && value <= 440) {
                    animateZapping(zap1, 0f, coreOffset, 1f, infuser)!!.light(light).renderInto(
                        ms, buffer.getBuffer(
                            RenderType.translucentNoCrumbling()
                        )
                    )
                }
                if (value >= 220 && value <= 260 || value >= 360 && value <= 400 || value >= 400 && value <= 420) {
                    animateZapping(zap2, 120f, coreOffset, 2f, infuser)!!.light(light).renderInto(
                        ms, buffer.getBuffer(
                            RenderType.translucentNoCrumbling()
                        )
                    )
                }
                if (value >= 240 && value <= 280 || value >= 320 && value <= 360 || value >= 410 && value <= 430) {
                    animateZapping(zap3, 240f, coreOffset, 3f, infuser)!!.light(light).renderInto(
                        ms, buffer.getBuffer(
                            RenderType.translucentNoCrumbling()
                        )
                    )
                }
            }
            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.DISASSEMBLY) {
                val value = infuser.disassemblyProgress.value
                //animateDisassembly(core, angle, offset, value).light(light).renderInto(ms, vb);
            }
            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.IDLE) {
                idleRotateCore(core, angle, offset, infuser).light(light).renderInto(ms, vb)
            }
        }
    }

    private fun idleRotateCore(
        buffer: SuperByteBuffer,
        angle: Float,
        offset: Float,
        infuser: PhysicsInfuserBlockEntity
    ): SuperByteBuffer {
        val pivotX = 8f
        val pivotY = 8f
        val pivotZ = 8f
        val speen = LerpedFloat.linear()
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        val q = Quaternion(0f, 1f, 0f, angle)
        buffer.rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
            .translate(0.0, offset.toDouble(), 0.0)
        return buffer
    }

    private fun animateZapping(
        buffer: SuperByteBuffer?,
        angle: Float,
        coreOffset: Float,
        value: Float,
        infuser: PhysicsInfuserBlockEntity
    ): SuperByteBuffer? {
        val pivotX = 8f / 16f
        val pivotY = 0f
        val pivotZ = 8f / 16f
        val transX: Float
        val transZ: Float
        if (value == 1f) {
            transX = 0f
            transZ = -0.25f
        } else if (value == 2f) {
            transX = 0.25f
            transZ = 0.25f
        } else {
            transX = -0.25f
            transZ = 0.25f
        }
        buffer!!.translateY((coreOffset * 2 - 0.15f).toDouble())
        buffer.translate(pivotX.toDouble(), pivotY.toDouble(), pivotZ.toDouble())
        buffer.rotate(Direction.UP, (angle / 180 * Math.PI).toFloat())
        buffer.translate(transX.toDouble(), 0.0, transZ.toDouble())
        buffer.translate(-pivotX.toDouble(), -pivotY.toDouble(), -pivotZ.toDouble())
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
        buffer.translateY((coreOffset * 2).toDouble())
            .rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
        return buffer
    }

    @Environment(EnvType.CLIENT)
    inner class ScanManager {
        // --------------------------------------------------------------------- //
        fun computeScanGrowthDuration(): Int {
            return te!!.scanGrowthDuration
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