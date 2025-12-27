package org.valkyrienskies.clockwork.content.contraptions.propeller.copter

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials

class CopterBearingRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<CopterBearingBlockEntity>(context) {

    private val pistonNW: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NW
    private val pistonNE: PartialModel = ClockworkPartials.SMART_PROP_PISTON_NE
    private val pistonSW: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SW
    private val pistonSE: PartialModel = ClockworkPartials.SMART_PROP_PISTON_SE

    private val top: PartialModel = ClockworkPartials.SMART_PROP_TOP

    private val wafer: PartialModel = ClockworkPartials.SMART_PROP_WAFER

    private var prevRotationQuat: Quaternionf = Quaternionf()

    override fun renderSafe(
        blockEntity: CopterBearingBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {

        //if (Backend.canUseInstancing(blockEntity.level)) return

        //super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        val facing: Direction = blockEntity.blockState.getValue(BlockStateProperties.FACING)
        val normal = Vec3(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())

        //val target = blockEntity.clientTargetTiltQuat

        //val interpol = MathUtil.nlerp(prevRotationQuat, target, partialTicks)

        blockEntity.clientTiltQuat =
            blockEntity.clientTiltQuat.slerp(blockEntity.clientTargetTiltQuat, partialTicks / 2f)
        /*
        val formattedPrev = String.format("(%.3f, %.3f, %.3f)",
            quat.x, quat.y,
            quat.z)
        val formattedQuat = String.format("(%.3f, %.3f, %.3f)",
            targetQuat.x, targetQuat.y,
            targetQuat.z)
        val formattedInterpol = String.format("(%.3f, %.3f, %.3f)",
            resultVec.x, resultVec.y,
            resultVec.z)


        println("Interpolating quaternion: ${String.format("%.3f", partialTicks)} \nprev=$formattedPrev, \ninte=$formattedInterpol, \ntarg=$formattedQuat")
         */


        //Render Pistons
        renderPistons(ms, buffer, blockEntity, light)

        //Render Top
        renderTop(ms, buffer, blockEntity, normal, blockEntity.clientTiltQuat, facing, partialTicks, light)

        //Render Wafer
        renderWafer(ms, buffer, blockEntity, normal, blockEntity.clientTiltQuat, facing, light)
    }

    private fun renderTop(
        ms: PoseStack,
        buffer: MultiBufferSource,
        blockEntity: CopterBearingBlockEntity,
        normal: Vec3,
        tiltQuaternion: Quaternionf,
        facing: Direction,
        partialTicks: Float,
        light: Int
    ) {
        val superBuffer = CachedBuffers.partial(top, blockEntity.blockState)

        superBuffer.translate(normal.scale(0.1))
        superBuffer.rotateCentered(tiltQuaternion)
        superBuffer.translate(normal.scale(-0.1))

        val interpolatedAngle: Float = blockEntity.getInterpolatedAngle(partialTicks - 1)
        kineticRotationTransform(superBuffer, blockEntity, facing.axis, (interpolatedAngle / 180 * Math.PI).toFloat(), light)

        superBuffer.rotateCentered(AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()), Direction.EAST)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    private fun renderPistons(
        ms: PoseStack,
        buffer: MultiBufferSource,
        blockEntity: CopterBearingBlockEntity,
        light: Int
    ) {
        var superBuffer: SuperByteBuffer = CachedBuffers.partial(pistonNW, blockEntity.blockState)
        superBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBuffers.partial(pistonNE, blockEntity.blockState)
        superBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBuffers.partial(pistonSW, blockEntity.blockState)
        superBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        superBuffer = CachedBuffers.partial(pistonSE, blockEntity.blockState)
        superBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    private fun renderWafer(
        ms: PoseStack,
        buffer: MultiBufferSource,
        blockEntity: CopterBearingBlockEntity,
        normal: Vec3,
        tiltQuaternion: Quaternionf,
        facing: Direction,
        light: Int
    ) {
        val superBuffer = CachedBuffers.partial(wafer, blockEntity.blockState)

        superBuffer.translate(normal.scale(0.1))
        superBuffer.rotateCentered(tiltQuaternion)
        superBuffer.translate(normal.scale(-0.1))

        superBuffer.rotateCentered(AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()), Direction.EAST)
        superBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

}
