package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
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
        if (infuser.animationType != null) {
            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.ASSEMBLY) {
                val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
                val coreOffset = te.getCoreOffset(partialTicks - 1)

                val innerData = TransformData(Vector3f(0f, coreOffset, 0f), Vector3f(interpolatedAngle, interpolatedAngle, 0f))
                val data = TransformData(Vector3f(0f, coreOffset, 0f), Vector3f(0f, interpolatedAngle, 0f))
                val outerData = TransformData(Vector3f(0f, coreOffset, 0f), Vector3f(0f, interpolatedAngle, 0f))

                RenderUtil.renderCubeMatrix(ms, buffer, blockState, innerData, data, outerData, 1.5f, light,overlay)
            }

            if (infuser.animationType === PhysicsInfuserBlockEntity.Animation.IDLE) {

                val interpolatedAngle = infuser.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)

                val innerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, interpolatedAngle, 0f))
                val data = TransformData(Vector3f(0f, 0f, 0f), Vector3f(0f, interpolatedAngle, 0f))
                val outerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(0f, interpolatedAngle, 0f))

                RenderUtil.renderCubeMatrix(ms, buffer, blockState, innerData, data, outerData,1.5f, light,overlay)
            }
        }
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
