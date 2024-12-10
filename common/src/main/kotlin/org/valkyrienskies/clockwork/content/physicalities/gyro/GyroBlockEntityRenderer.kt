package org.valkyrienskies.clockwork.content.physicalities.gyro

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
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
        val angle: Float = be.angle + speed * partialTicks



        val vb = buffer!!.getBuffer(RenderType.solid())
        renderGyro(be, ms, light, blockState, angle, vb)

        renderCore(be, ms, light, blockState, buffer)

        val indicator = CachedBufferer.partial(ClockworkPartials.GYRO_BASE, blockState)
        indicator
            .light(light)
            .color(255,255,255,255)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))

        ms.pushPose()
        val shaf = getRotatedModel(be, blockState)
        coolKineticRotationTransform(shaf, be, getAngleForTe(be, be.blockPos, Direction.Axis.Y), light).renderInto(ms, vb)
        ms.popPose()
    }


    fun coolKineticRotationTransform(buffer: SuperByteBuffer, be: KineticBlockEntity, angle: Float, light: Int): SuperByteBuffer {
        buffer.light(light)

        buffer.rotate(-90.0, Direction.Axis.X)
        buffer.translate(0.0,-1.0,0.0)

        var axlDir = Direction.AxisDirection.POSITIVE
        var axl = Direction.Axis.Z

        buffer.rotateCentered(Direction.get(axlDir, axl), angle)

        return buffer
    }

    private fun renderCore(be: GyroBlockEntity, ms: PoseStack?, light: Int, blockState: BlockState, buffer: MultiBufferSource) {
        val interpolatedAngle = be.getInterpolatedCoreAngle(com.simibubi.create.foundation.utility.AnimationTickHolder.getPartialTicks() - 1)

        val innerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, interpolatedAngle, 0f))
        val data = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, 0f, -interpolatedAngle))
        val outerData = TransformData(Vector3f(0f, 0f, 0f), Vector3f(interpolatedAngle, 0f, -interpolatedAngle))

        RenderUtil.renderCubeMatrix(ms!!, buffer, blockState, innerData, data, outerData, 1.5f, light)

    }

    private fun renderGyro(be: GyroBlockEntity, ms: PoseStack?, light: Int, blockState: BlockState, angle: Float, vb: VertexConsumer) {
        val wheel = CachedBufferer.block(blockState)

        //TODO this also eed to rotate with ship rotation to make sense, also maybe invert tilt to make more sense
        wheel.translate(0.45,0.0,0.45)
        wheel.multiply(com.mojang.math.Vector3f.ZN, be.targetQuat.x() * 45)
        //wheel.multiply(com.mojang.math.Vector3f.YP, be.targetQuat.y() * 45)
        wheel.multiply(com.mojang.math.Vector3f.XP, be.targetQuat.z() * 45)
        wheel.translate(-0.45,0.0,-0.45)

        kineticRotationTransform(wheel, be, getRotationAxisOf(be), AngleHelper.rad(angle.toDouble()), light)
        wheel.translate(0.0,1.0,0.0)

        wheel.renderInto(ms, vb)
    }

    override fun getRotatedModel(te: GyroBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            ClockworkPartials.PHYS_SHAFT, state, Direction.DOWN
        )
    }
}