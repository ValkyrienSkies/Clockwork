package org.valkyrienskies.clockwork.content.contraptions.propeller

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkPartials

class PropellerBearingRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<PropellerBearingBlockEntity>(context) {

    override fun renderSafe(
        te: PropellerBearingBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // if (Backend.canUseInstancing(te.getLevel())) return;
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
        val bearingTe: PropellerBearingBlockEntity = te as PropellerBearingBlockEntity
        renderRotatingBuffer(te, getRotatedModel(te, te.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)
        val facing: Direction = te.blockState.getValue(BlockStateProperties.FACING)
        val top = ClockworkPartials.PROPELLER_TOP
        val superBuffer = CachedBufferer.partial(top, te.getBlockState())
        ms.pushPose()
        ms.translate(0.5, 0.5, 0.5)
        ms.mulPose(Quaternionf().rotationXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))
        val ogfacing = te.blockState.getValue(BlockStateProperties.FACING)
        when (ogfacing) {
            Direction.SOUTH -> ms.mulPose(Axis.XP.rotationDegrees(270f))
            Direction.WEST -> ms.mulPose(Axis.ZP.rotationDegrees(270f))
            Direction.NORTH -> ms.mulPose(Axis.XP.rotationDegrees(90f))
            Direction.EAST -> ms.mulPose(Axis.ZP.rotationDegrees(90f))
            Direction.UP -> ms.mulPose(Axis.XP.rotationDegrees(0f))
            Direction.DOWN -> ms.mulPose(Axis.XN.rotationDegrees(180f))
        }
        ms.translate(-0.5, -0.5, -0.5)
        val pistonTopL = CachedBufferer.partial(ClockworkPartials.PROPELLER_PISTON_TOP_LEFT, te.blockState)
        val pistonTopR = CachedBufferer.partial(ClockworkPartials.PROPELLER_PISTON_TOP_RIGHT, te.blockState)
        val pistonBotL = CachedBufferer.partial(ClockworkPartials.PROPELLER_PISTON_BOTTOM_LEFT, te.blockState)
        val pistonBotR = CachedBufferer.partial(ClockworkPartials.PROPELLER_PISTON_BOTTOM_RIGHT, te.blockState)
        val interpolatedAngle: Float = bearingTe.getInterpolatedAngle(partialTicks - 1)
        kineticRotationTransform(
            superBuffer,
            te,
            Direction.UP.axis,
            (interpolatedAngle / 180 * Math.PI).toFloat(),
            light
        )
        shakeEngine(pistonTopL, te.rotspeed, partialTicks, facing, te, 1)
        shakeEngine(pistonTopR, te.rotspeed, partialTicks, facing, te, 2)
        shakeEngine(pistonBotL, te.rotspeed, partialTicks, facing, te, 3)
        shakeEngine(pistonBotR, te.rotspeed, partialTicks, facing, te, 4)

        if (facing.axis.isHorizontal) superBuffer.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        superBuffer.rotateCentered(
            Direction.UP,
            AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        pistonTopL.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        pistonTopR.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        pistonBotL.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        pistonBotR.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )

        pistonTopL.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        pistonTopR.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        pistonBotR.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))
        pistonBotL.rotateCentered(Direction.EAST, AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble()))

        pistonTopL.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()))
        pistonTopR.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()))
        pistonBotL.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()))
        pistonBotR.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()))
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))


        ms.popPose()
    }

    override fun getRotatedModel(te: PropellerBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }

    private fun shakeEngine(
        buffer: SuperByteBuffer,
        speed: Float,
        partialTicks: Float,
        facing: Direction,
        te: PropellerBearingBlockEntity,
        ordinal: Int
    ): SuperByteBuffer {
        // Clamp speed to be at most 48 (because of clipping issues)
        var speed = speed
        speed = Mth.clamp(speed, -48.0f, 48.0f)

        val xSwitch = when (ordinal) {
            1 -> 1
            2 -> -1
            3 -> 1
            4 -> -1
            else -> 1
        }
        val zSwitch = when (ordinal) {
            1 -> 1
            2 -> 1
            3 -> -1
            4 -> -1
            else -> 1
        }

        val interpolatedHorizontalOffset =
            te.getCornerHorizontalOffset(AnimationTickHolder.getPartialTicks() - 1, te, ordinal)
        val verticalOffset = when (ordinal) {
            1 -> 1.5 / 16.0
            2 -> 1.5 / 16.0
            3 -> 0.0
            4 -> 0.0
            else -> 0.0
        }
        val translate = when (facing) {
            Direction.UP -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                verticalOffset,
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )

            Direction.NORTH -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                verticalOffset
            )

            Direction.SOUTH -> Vec3(
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                verticalOffset
            )

            Direction.EAST -> Vec3(
                verticalOffset,
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )

            Direction.WEST -> Vec3(
                verticalOffset,
                (interpolatedHorizontalOffset * zSwitch).toDouble(),
                (interpolatedHorizontalOffset * xSwitch).toDouble()
            )

            Direction.DOWN -> Vec3(
                (interpolatedHorizontalOffset * xSwitch).toDouble(),
                verticalOffset,
                (interpolatedHorizontalOffset * zSwitch).toDouble()
            )
        }
        buffer.translate(translate)
        return buffer
    }
}