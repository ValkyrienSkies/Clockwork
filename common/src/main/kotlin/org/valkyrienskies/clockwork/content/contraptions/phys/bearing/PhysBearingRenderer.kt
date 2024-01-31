package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.flywheel.FlywheelRenderer
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData
import kotlin.math.PI

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class PhysBearingRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<PhysBearingBlockEntity>(context) {



    override fun getRotatedModel(te: PhysBearingBlockEntity, state: BlockState): SuperByteBuffer {
        var v =  getRotationAxisOf(te)

        return CachedBufferer.partialFacing(
            ClockworkPartials.PHYS_ATTACHER, state, state.getValue(BearingBlock.FACING).opposite
        )
    }

    override fun renderSafe(blockEntity: PhysBearingBlockEntity, partialTicks: Float, matrices: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        val blockState = blockEntity.blockState
        val facing = blockState.getValue(BlockStateProperties.FACING)

        val phys_north = CachedBufferer.partial(ClockworkPartials.PHYS_NORTH_WING, blockState)
        val phys_south = CachedBufferer.partial(ClockworkPartials.PHYS_SOUTH_WING, blockState)
        val phys_west = CachedBufferer.partial(ClockworkPartials.PHYS_WEST_WING, blockState)
        val phys_east = CachedBufferer.partial(ClockworkPartials.PHYS_EAST_WING, blockState)

        //val shaft = CachedBufferer.partialFacing(ClockworkPartials.PHYS_SHAFT, blockState, facing)
        //val attacher = CachedBufferer.partialFacing(ClockworkPartials.PHYS_ATTACHER, blockState, facing)


        //Render Shaft and Attacher
        matrices.pushPose()
        //matrices.mulPose(Vector3f.ZP.rotationDegrees(90f))
        //renderRotatingBuffer(blockEntity, shaft, matrices, buffer.getBuffer(RenderType.solid()), light)
        //renderRotatingBuffer(blockEntity, attacher, matrices, buffer.getBuffer(RenderType.solid()), light)

        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Quaternion.fromXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(270f))
            Direction.WEST -> matrices.mulPose(Vector3f.ZP.rotationDegrees(270f))
            Direction.NORTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(90f))
            Direction.EAST -> matrices.mulPose(Vector3f.ZP.rotationDegrees(90f))
            Direction.UP -> matrices.mulPose(Vector3f.XP.rotationDegrees(0f))
            Direction.DOWN -> matrices.mulPose(Vector3f.XN.rotationDegrees(180f))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        matrices.pushPose()
        var v = getRotatedModel(blockEntity, blockState)
        //v.rotateCentered(Quaternion(Vector3f(0f,-1f,0f),90f, true))
        matrices.mulPose(Vector3f.YN.rotationDegrees(90f))
        renderRotatingBuffer(blockEntity, v, matrices, buffer.getBuffer(RenderType.solid()), light)
        matrices.popPose()


        //TODO render auric matrix

        //Render partials
        rotateAndRiseNorth(phys_north, blockEntity)
        rotateAndRiseSouth(phys_south, blockEntity)

        rotateWestWing(phys_west, blockEntity)
        rotateEastWing(phys_east, blockEntity)

        //translateAttacher(shaft, attacher, blockEntity, facing) //TODO implement

        val vertexConsumer = buffer.getBuffer(RenderType.translucent())

        phys_north.renderInto(matrices, vertexConsumer)
        phys_south.renderInto(matrices, vertexConsumer)
        phys_west.renderInto(matrices, vertexConsumer)
        phys_east.renderInto(matrices, vertexConsumer)

        matrices.popPose()
    }

    private fun rotateAndRiseNorth(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity) {
        val pivot = Vec3(0.0, -7.0 / 16,-5.5 / 16)//TODO find pivot
        rotateAndRise(physPartial, blockEntity, pivot, Direction.WEST)
    }

    private fun rotateAndRiseSouth(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity) {
        val pivot = Vec3(0.0,-7.0 / 16,5.5 / 16)//TODO find pivot
        rotateAndRise(physPartial, blockEntity, pivot, Direction.EAST)
    }

    private fun rotateAndRise(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity, pivot: Vec3, axl: Direction) {
        //TODO

        val interpolatedAngle = blockEntity.getWingRotOffset()

        physPartial.translate(pivot)
        physPartial.rotateCentered(axl, (interpolatedAngle / 360 * Math.PI).toFloat())
        physPartial.translateBack(pivot)
    }

    private fun rotateWestWing(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity) {
        val pivot = Vec3(- 7 / 16.0, 1.0 / 16, 0.0)
        rotateWing(physPartial, blockEntity, pivot, Direction.SOUTH)
    }

    private fun rotateEastWing(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity) {
        val pivot = Vec3(7 / 16.0, 1.0 / 16, 0.0)
        rotateWing(physPartial, blockEntity, pivot, Direction.NORTH)
    }

    private fun rotateWing(physPartial: SuperByteBuffer, blockEntity: PhysBearingBlockEntity, pivot: Vec3, axl: Direction) {
        val interpolatedAngle = blockEntity.getWingRotOffset()
        physPartial.translate(pivot)
        physPartial.rotateCentered(axl, (interpolatedAngle / 360 * Math.PI).toFloat())
        physPartial.translateBack(pivot)
    }

    private fun idleRotateCore(buffer: SuperByteBuffer, offset: Float, bearing: PhysBearingBlockEntity): SuperByteBuffer {
        val interpolatedAngle = bearing.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        buffer.rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat()).translate(0.0, offset.toDouble(), 0.0)
        return buffer
    }
}
