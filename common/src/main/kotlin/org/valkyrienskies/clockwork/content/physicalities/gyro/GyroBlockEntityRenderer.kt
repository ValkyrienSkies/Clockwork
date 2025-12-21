package org.valkyrienskies.clockwork.content.physicalities.gyro

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData


class GyroBlockEntityRenderer(context: BlockEntityRendererProvider.Context?) :
    KineticBlockEntityRenderer<GyroBlockEntity>(context) {

    private var crystalAngle = 0f

    override fun renderSafe(be: GyroBlockEntity, partialTicks: Float, ms: PoseStack, buffer: MultiBufferSource?, light: Int, overlay: Int) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val blockState: BlockState = be.blockState
        val speed: Float = be.visualSpeed.getValue(partialTicks) * 3 / 10f
        val angle: Double = be.angle + speed * partialTicks


        val vb = buffer!!.getBuffer(RenderType.solid())
        renderGyro(be, ms, light, blockState, angle, vb)

        renderCore(be, ms, light, blockState, buffer, overlay)

        val indicator = CachedBuffers.partial(ClockworkPartials.GYRO_BASE, blockState)
        indicator
            .light<SuperByteBuffer>(light)
            .color<SuperByteBuffer>(255,255,255,255)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))

        ms.pushPose()
        val shaf = getRotatedModel(be, blockState)
        coolKineticRotationTransform(shaf, be, getAngleForBe(be, be.blockPos, Direction.Axis.Y), light).renderInto(ms, vb)
        ms.popPose()
    }


    fun coolKineticRotationTransform(buffer: SuperByteBuffer, be: KineticBlockEntity, angle: Float, light: Int): SuperByteBuffer {
        buffer.light<SuperByteBuffer>(light)

        buffer.rotateDegrees(-90.0f, Direction.Axis.X)
        buffer.translate(0.0,-1.0,0.0)

        var axlDir = Direction.AxisDirection.POSITIVE
        var axl = Direction.Axis.Z

        buffer.rotateCentered(angle, Direction.get(axlDir, axl))

        return buffer
    }

    private fun renderCore(be: GyroBlockEntity, ms: PoseStack?, light: Int, blockState: BlockState, buffer: MultiBufferSource, overlay: Int) {
        val interpolatedAngle = be.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)

        val innerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, interpolatedAngle, 0f))
        val data = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, 0f, -interpolatedAngle))
        val outerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, 0f, -interpolatedAngle))

        RenderUtil.renderCubeMatrix(ms!!, buffer, blockState, innerData, data, outerData, 1.5f, light, overlay)

    }

    private fun renderGyro(be: GyroBlockEntity, ms: PoseStack?, light: Int, blockState: BlockState, angle: Double, vb: VertexConsumer) {
        val wheel = CachedBuffers.block(blockState)

        //TODO this also eed to rotate with ship rotation to make sense, also maybe invert tilt to make more sense
        wheel.translate(0.45,0.0,0.45)
        //wheel.(Vector3f(0f, 0f, -1f), be.targetQuat.x() * 45)
        //wheel.multiply(com.mojang.math.Vector3f.YP, be.targetQuat.y() * 45)
        //wheel.multiply(Vector3f(1f, 0f, 0f), be.targetQuat.z() * 45)
        wheel.translate(-0.45,0.0,-0.45)

        kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle.toDouble()), light)
        wheel.translate(0.0,1.0,0.0)

        wheel.renderInto(ms, vb)
    }

    override fun getRotatedModel(te: GyroBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBuffers.partialFacing(
            ClockworkPartials.PHYS_SHAFT, state, Direction.DOWN
        )
    }
}
