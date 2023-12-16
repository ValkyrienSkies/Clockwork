package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import com.mojang.blaze3d.vertex.PoseStack
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
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBi
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItem
import org.valkyrienskies.clockwork.util.render.Bolt
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.mod.common.util.toJOMLD

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
        val vb = buffer.getBuffer(RenderType.cutoutMipped())

        // Render Mysterious Liquid
        val mysteriousLiquid = CachedBufferer.partial(ClockworkPartials.STRANGE_FLUID, blockState)
        mysteriousLiquid.light(light).renderInto(ms, buffer.getBuffer(RenderType.translucent()))

        val designator: AuricDesignatorItem? = if (infuser.getItem(0).item is AuricDesignatorItem)  {
            infuser.getItem(0).item as AuricDesignatorItem
        } else {
            null
        }

        val selectionNearestPoints: ArrayList<Vector3dc> = ArrayList()

        designator?.selectedArea?.selectionClusters?.forEach { selection ->
            var maxX: Int = 0
            var maxY: Int = 0
            var maxZ: Int = 0
            var minX: Int = 0
            var minY: Int = 0
            var minZ: Int = 0
            selection.forEach {
                maxX += it.maxX()
                maxY += it.maxY()
                maxZ += it.maxZ()
                minX += it.minX()
                minY += it.minY()
                minZ += it.minZ()
            }
            maxX /= selection.size
            maxY /= selection.size
            maxZ /= selection.size
            minX /= selection.size
            minY /= selection.size
            minZ /= selection.size
            val selectionCenter: Vector3dc = AABBi(minX, minY, minZ, maxX, maxY, maxZ).center(Vector3d())
            selectionNearestPoints.add(selectionCenter)
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

    private fun idleRotateCore(buffer: SuperByteBuffer, angle: Float, offset: Float, infuser: PhysicsInfuserBlockEntity): SuperByteBuffer {
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        buffer.rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat()).translate(0.0, offset.toDouble(), 0.0)
        return buffer
    }


    private fun animateAssembly(buffer: SuperByteBuffer, angle: Float, coreOffset: Float, value: Float, infuser: PhysicsInfuserBlockEntity): SuperByteBuffer {
        val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        buffer.translateY((coreOffset * 2).toDouble()).rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
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