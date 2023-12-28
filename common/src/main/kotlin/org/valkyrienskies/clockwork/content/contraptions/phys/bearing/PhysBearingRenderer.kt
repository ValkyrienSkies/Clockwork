package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData

class PhysBearingRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<PhysBearingBlockEntity>(context) {
    override fun renderSafe(
        te: PhysBearingBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        val pte = te
        val ogfacing = te.blockState
            .getValue(BlockStateProperties.FACING)
        renderRotatingBuffer(te, getRotatedModel(te, te.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)
        val facing = Direction.UP
        ms.pushPose()
        ms.translate(0.5, 0.5, 0.5)
        ms.mulPose(Quaternion.fromXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))
        when (ogfacing) {
            Direction.SOUTH -> ms.mulPose(Vector3f.XP.rotationDegrees(270f))
            Direction.WEST -> ms.mulPose(Vector3f.ZP.rotationDegrees(270f))
            Direction.NORTH -> ms.mulPose(Vector3f.XP.rotationDegrees(90f))
            Direction.EAST -> ms.mulPose(Vector3f.ZP.rotationDegrees(90f))
            Direction.UP -> ms.mulPose(Vector3f.XP.rotationDegrees(0f))
            Direction.DOWN -> ms.mulPose(Vector3f.XN.rotationDegrees(180f))
        }
        ms.translate(-0.5, -0.5, -0.5)
        val blockState = te.blockState
        //val core = CachedBufferer.partial(ClockworkPartials.PHYSICS_CORE, blockState)
        val flapNorth = CachedBufferer.partial(ClockworkPartials.PHYSFLAP_NORTH, blockState)
        val flapSouth = CachedBufferer.partial(ClockworkPartials.PHYSFLAP_SOUTH, blockState)
        val flapEast = CachedBufferer.partial(ClockworkPartials.PHYSFLAP_EAST, blockState)
        val flapWest = CachedBufferer.partial(ClockworkPartials.PHYSFLAP_WEST, blockState)
        val cornerNE = CachedBufferer.partial(ClockworkPartials.PHYSCORNER_NE, blockState)
        val cornerNW = CachedBufferer.partial(ClockworkPartials.PHYSCORNER_NW, blockState)
        val cornerSE = CachedBufferer.partial(ClockworkPartials.PHYSCORNER_SE, blockState)
        val cornerSW = CachedBufferer.partial(ClockworkPartials.PHYSCORNER_SW, blockState)



        //RENDER CRYSTAL MATRIX start
        val interpolatedAngle = te.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        var offset = when(ogfacing) {
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

        RenderUtil.renderCubeMatrix(ms, buffer, blockState, innerData, data, outerData, 1.5f,light)
        val vb = buffer.getBuffer(RenderType.translucent())
        //RENDER CRYSTAL MATRIX end



        rotateFlap(flapNorth, pte, 1, facing)
        rotateFlap(flapEast, pte, 2, facing)
        rotateFlap(flapSouth, pte, 3, facing)
        rotateFlap(flapWest, pte, 4, facing)
        translateCorner(cornerNE, pte, 1, facing)
        translateCorner(cornerNW, pte, 2, facing)
        translateCorner(cornerSE, pte, 3, facing)
        translateCorner(cornerSW, pte, 4, facing)
        if (facing.axis.isHorizontal) {

            flapNorth.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            flapEast.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            flapSouth.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            flapWest.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            cornerNE.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            cornerNW.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            cornerSE.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
            cornerSW.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
        }
        //core.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        flapEast.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        flapNorth.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        flapSouth.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        flapWest.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        cornerNE.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        cornerSW.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        cornerSE.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        cornerNW.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        //core.renderInto(ms, vb)
        flapNorth.renderInto(ms, vb)
        flapEast.renderInto(ms, vb)
        flapSouth.renderInto(ms, vb)
        flapWest.renderInto(ms, vb)
        cornerNE.renderInto(ms, vb)
        cornerNW.renderInto(ms, vb)
        cornerSE.renderInto(ms, vb)
        cornerSW.renderInto(ms, vb)



        ms.popPose()
    }

    override fun getRotatedModel(te: PhysBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }

    private fun idleRotateCore(
        buffer: SuperByteBuffer,
        offset: Float,
        bearing: PhysBearingBlockEntity
    ): SuperByteBuffer {
        val speen = LerpedFloat.linear()
        val interpolatedAngle = bearing.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        buffer.rotateCentered(Direction.UP, (interpolatedAngle / 180 * Math.PI).toFloat())
            .translate(0.0, offset.toDouble(), 0.0)
        return buffer
    }

    private fun rotateFlap(
        buffer: SuperByteBuffer,
        bearing: PhysBearingBlockEntity,
        ordinal: Int,
        facing: Direction
    ): SuperByteBuffer {
        var pivotX = 8f
        var pivotY = 8f
        var pivotZ = 8f
        var direction = bearing.blockState.getValue(BlockStateProperties.FACING)
        when (ordinal) {
            1 -> {
                pivotX = 0f
                pivotY = 2 / 16f
                pivotZ = -6 / 16f
                direction = Direction.WEST
            }

            2 -> {
                pivotX = 5 / 16f
                pivotY = 2 / 16f
                pivotZ = 0f
                direction = Direction.NORTH
            }

            3 -> {
                pivotX = 0f
                pivotY = 2 / 16f
                pivotZ = 6 / 16f
                direction = Direction.EAST
            }

            4 -> {
                pivotX = -5 / 16f
                pivotY = 2 / 16f
                pivotZ = 0f
                direction = Direction.SOUTH
            }
        }
        val pivot = when (facing) {
            Direction.UP -> Vec3(pivotX.toDouble(), pivotY.toDouble(), pivotZ.toDouble())
            Direction.NORTH -> Vec3(pivotX.toDouble(), pivotZ.toDouble(), pivotY.toDouble())
            Direction.SOUTH -> Vec3(pivotX.toDouble(), pivotZ.toDouble(), -pivotY.toDouble())
            Direction.EAST -> Vec3(pivotY.toDouble(), pivotX.toDouble(), pivotZ.toDouble())
            Direction.WEST -> Vec3(-pivotY.toDouble(), pivotX.toDouble(), pivotZ.toDouble())
            Direction.DOWN -> Vec3(-pivotX.toDouble(), -pivotY.toDouble(), -pivotZ.toDouble())
        }
        val interpolatedAngle = bearing.getFlapRotOffset(AnimationTickHolder.getPartialTicks() - 1)
        //        buffer.translate(-pivotX, -pivotY, -pivotZ);
        buffer.translate(pivot)
        buffer.rotateCentered(direction, (interpolatedAngle / 180 * Math.PI).toFloat())
        buffer.translateBack(pivot)
        return buffer
    }

    private fun translateCorner(
        buffer: SuperByteBuffer,
        bearing: PhysBearingBlockEntity,
        ordinal: Int,
        facing: Direction
    ): SuperByteBuffer {
        val xSwitch = when (ordinal) {
            1 -> 1
            2 -> -1
            3 -> 1
            4 -> -1
            else -> 1
        }
        val zSwitch = when (ordinal) {
            1 -> -1
            2 -> -1
            3 -> 1
            4 -> 1
            else -> 1
        }
        val interpolatedHorizontalOffset = bearing.getCornerHorizontalOffset(AnimationTickHolder.getPartialTicks() - 1)
        val interpolatedVerticalOffset = bearing.getCornerVerticalOffset(AnimationTickHolder.getPartialTicks() - 1)
        val translate = when (facing) {
            Direction.UP -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                interpolatedVerticalOffset.toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )

            Direction.NORTH -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                -interpolatedVerticalOffset.toDouble()
            )

            Direction.SOUTH -> Vec3(
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                interpolatedVerticalOffset.toDouble()
            )

            Direction.EAST -> Vec3(
                -interpolatedVerticalOffset.toDouble(),
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )

            Direction.WEST -> Vec3(
                interpolatedVerticalOffset.toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                (interpolatedHorizontalOffset * xSwitch).toDouble()
            )

            Direction.DOWN -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                -interpolatedVerticalOffset.toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )
        }
        buffer.translate(translate)
        return buffer
    }
}
