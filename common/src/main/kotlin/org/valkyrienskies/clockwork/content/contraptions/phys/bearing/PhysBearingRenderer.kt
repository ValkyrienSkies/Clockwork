package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.clock.CuckooClockBlockEntity
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AnimationTickHolder
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

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class PhysBearingRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<PhysBearingBlockEntity>(context) {

    override fun renderSafe(blockEntity: PhysBearingBlockEntity?, partialTicks: Float, matrices: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(blockEntity, partialTicks, matrices, buffer, light, overlay)

        if (blockEntity !is PhysBearingBlockEntity) return

        val blockState = blockEntity.blockState
        val facing = blockState.getValue(BlockStateProperties.FACING)

        val phys_north = CachedBufferer.partial(ClockworkPartials.PHYS_NORTH_WING, blockState)
        val phys_south = CachedBufferer.partial(ClockworkPartials.PHYS_SOUTH_WING, blockState)
        val phys_west = CachedBufferer.partial(ClockworkPartials.PHYS_WEST_WING, blockState)
        val phys_east = CachedBufferer.partial(ClockworkPartials.PHYS_EAST_WING, blockState)


        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Quaternion.fromXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(270f))
            Direction.WEST -> {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(270f))
                matrices.mulPose(Vector3f.YP.rotationDegrees(90f))
            }
            Direction.NORTH -> matrices.mulPose(Vector3f.XP.rotationDegrees(90f))
            Direction.EAST -> {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(90f))
                matrices.mulPose(Vector3f.YP.rotationDegrees(90f))
            }
            Direction.UP -> matrices.mulPose(Vector3f.XP.rotationDegrees(0f))
            Direction.DOWN -> matrices.mulPose(Vector3f.XN.rotationDegrees(180f))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        //TODO render auric matrix
        //RENDER CRYSTAL MATRIX start
        val interpolatedAngle = blockEntity.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        var offset = when(facing) {
            Direction.SOUTH -> org.joml.Vector3f(0.0f,0.0f,0.1f)
            Direction.WEST -> org.joml.Vector3f(0.1f,0.0f,0.0f)
            Direction.NORTH -> org.joml.Vector3f(0.0f,0.0f,0.1f)
            Direction.EAST -> org.joml.Vector3f(0.1f,0.0f,0.0f)
            Direction.UP -> org.joml.Vector3f(0.0f,0.1f,0.0f)
            Direction.DOWN -> org.joml.Vector3f(0.0f,0.1f,0.0f)
        }
        val innerData = TransformData(offset, org.joml.Vector3f(interpolatedAngle, interpolatedAngle, 0f))
        val data = TransformData(offset, org.joml.Vector3f(0f, 0f, 0f))
        val outerData = TransformData(offset, org.joml.Vector3f(0f, 0f, 0f))

        RenderUtil.renderCubeMatrix(matrices, buffer, blockState, innerData, data, outerData, 1.5f,light)
        val vb = buffer.getBuffer(RenderType.translucent())
        //RENDER CRYSTAL MATRIX end

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

        matrices.pushPose()

        val at = getRotatedModelAttacher(blockEntity, blockEntity.blockState)
        val shaf = getRotatedModel(blockEntity, blockEntity.blockState)

        sickoModeRotationTransform(at, blockEntity, light).renderInto(matrices, vertexConsumer)
        sickoModeRotationTransform(shaf, blockEntity, light).renderInto(matrices, vertexConsumer)

        matrices.popPose()
    }

    fun sickoModeRotationTransform(buffer: SuperByteBuffer, be: KineticBlockEntity, light: Int): SuperByteBuffer {
        val pos = be.blockPos
        val axis = (be.blockState.block as IRotate).getRotationAxis(be.blockState)

        return coolKineticRotationTransform(buffer, be, getAngleForTe(be, pos, axis), light)
    }

    fun coolKineticRotationTransform(buffer: SuperByteBuffer, be: KineticBlockEntity, angle: Float, light: Int): SuperByteBuffer {
        buffer.light(light)
        val facing = be.blockState.getValue(BlockStateProperties.FACING)

        when (facing) {
            Direction.DOWN, Direction.UP -> {
                buffer.rotate(90.0, Direction.Axis.X)
                buffer.translate(0.0,0.0,-1.0)
            }
            Direction.NORTH -> {
                buffer.rotate(-90.0, Direction.Axis.X)
                buffer.translate(0.0,-1.0,0.0)
            }
            Direction.SOUTH -> {
                buffer.rotate(90.0, Direction.Axis.X)
                buffer.translate(0.0,0.0,-1.0)
            }
            Direction.WEST -> {
                buffer.rotate(90.0, Direction.Axis.Z)
                buffer.translate(0.0,-1.0,0.0)
            }
            Direction.EAST -> {
                buffer.rotate(-90.0, Direction.Axis.Z)
                buffer.translate(-1.0,0.0,0.0)
            }
        }

        var axlDir = Direction.AxisDirection.NEGATIVE
        var axl = Direction.Axis.Y

        if (facing == Direction.UP || facing == Direction.DOWN) {
            axl = Direction.Axis.Z
        }

        if (facing == Direction.EAST || facing == Direction.SOUTH) {
            axlDir = Direction.AxisDirection.POSITIVE
        }

        buffer.rotateCentered(Direction.get(axlDir, axl), angle)

        return buffer
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

    override fun getRotatedModel(te: PhysBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            ClockworkPartials.PHYS_SHAFT, state, state.getValue(BlockStateProperties.FACING)
        )
    }

    fun getRotatedModelAttacher(te: PhysBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            ClockworkPartials.PHYS_ATTACHER, state, state.getValue(BlockStateProperties.FACING)
        )
    }
}
