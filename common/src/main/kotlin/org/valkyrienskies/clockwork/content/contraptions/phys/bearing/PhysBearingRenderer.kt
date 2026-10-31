package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.animation.AnimationChannel
import net.minecraft.client.animation.AnimationDefinition
import net.minecraft.client.animation.Keyframe
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.util.ClockworkAnimationUtils
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.TransformData

@Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
class PhysBearingRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<PhysBearingBlockEntity>(context) {

    var orb_offset = 0f

    override fun renderSafe(blockEntity: PhysBearingBlockEntity?, partialTicks: Float, matrices: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        super.renderSafe(blockEntity, partialTicks, matrices, buffer, light, overlay)

        if (blockEntity !is PhysBearingBlockEntity) return

        val blockState = blockEntity.blockState
        val facing = blockState.getValue(BlockStateProperties.FACING)

        val phys_north = CachedBuffers.partial(ClockworkPartials.PHYS_NORTH_WING, blockState).light<SuperByteBuffer>(light)
        val phys_south = CachedBuffers.partial(ClockworkPartials.PHYS_SOUTH_WING, blockState).light<SuperByteBuffer>(light)
        val phys_west = CachedBuffers.partial(ClockworkPartials.PHYS_WEST_WING, blockState).light<SuperByteBuffer>(light)
        val phys_east = CachedBuffers.partial(ClockworkPartials.PHYS_EAST_WING, blockState).light<SuperByteBuffer>(light)
        val phys_shaft = CachedBuffers.partial(ClockworkPartials.PHYS_SHAFT, blockState).light<SuperByteBuffer>(light)
        val phys_attacher = CachedBuffers.partial(ClockworkPartials.PHYS_ATTACHER, blockState).light<SuperByteBuffer>(light)

        matrices.pushPose()
        matrices.translate(0.5, 0.5, 0.5)
        matrices.mulPose(Quaternionf().rotateXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))

        when (facing) {
            Direction.SOUTH -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 1f, 0f, 0f)))
            Direction.WEST -> {
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(270.0), 0f, 0f, 1f)))
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.NORTH -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 1f, 0f, 0f)))
            Direction.EAST -> {
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 0f, 1f)))
                matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(90.0), 0f, 1f, 0f)))
            }
            Direction.UP -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(0.0), 1f, 0f, 0f)))
            Direction.DOWN -> matrices.mulPose(Quaternionf(AxisAngle4f(AngleHelper.rad(180.0), -1f, 0f, 0f)))
        }

        matrices.translate(-0.5, -0.5, -0.5)

        val vertexConsumer = buffer.getBuffer(RenderType.translucent())

        //Render partials

        val axis = (blockState.block as IRotate).getRotationAxis(blockState)
        val angle = getAngleForBe(blockEntity, blockEntity.blockPos, axis);

        val t = blockEntity.getAnimProgress()
        val animationUtils = ClockworkAnimationUtils()
        val animation = if (blockEntity.isRunning) {
            PhysBearingAnimations.opening!!
        } else {
            PhysBearingAnimations.closing!!
        }
        val animationTime = if (blockEntity.isRunning) t else 1.35f - t

        if (blockEntity.isRunning) {
            animationUtils.applyAnimationToPartial(phys_west, animation, "westWing", Vec3(-7.0 / 16, 10.0 / 16, 0.0), animationTime)
            animationUtils.applyAnimationToPartial(phys_east, animation, "eastWing", Vec3(7.0 / 16, 10.0 / 16, 0.0), animationTime)
            animationUtils.applyAnimationToPartial(phys_north, animation, "southWing", Vec3(0.0, 3.0 / 16,-7.0 / 16), animationTime)
            animationUtils.applyAnimationToPartial(phys_south, animation, "northWing", Vec3(0.0,3.0 / 16,7.0 / 16), animationTime)
            animationUtils.applyAnimationToPartial(phys_attacher, animation, "attacher", Vec3(0.0,3.0 / 16,7.0 / 16), animationTime)
        }
        else {
            animationUtils.applyAnimationToPartial(phys_west, animation, "westWing", Vec3(-7.0 / 16, 10.0 / 16, 0.0), animationTime)
            animationUtils.applyAnimationToPartial(phys_east, animation, "eastWing", Vec3(7.0 / 16, 10.0 / 16, 0.0), animationTime)
            animationUtils.applyAnimationToPartial(phys_north, animation, "southWing", Vec3(0.0, 3.0 / 16, -7.0 / 16), animationTime)
            animationUtils.applyAnimationToPartial(phys_south, animation, "northWing", Vec3(0.0, 3.0 / 16, 7.0 / 16), animationTime)
            animationUtils.applyAnimationToPartial(phys_attacher, animation, "attacher", Vec3(0.0,3.0 / 16,7.0 / 16), animationTime)
        }

        val attacherPosition = animation.boneAnimations["attacher"]
            ?.firstOrNull { it.target == AnimationChannel.Targets.POSITION }
            ?.let { animationUtils.interpolateKeyframes(it.keyframes.toList(), animationTime) }
            ?: Vector3f()
        orb_offset = -attacherPosition.y / 16f

        RotateShaft(phys_shaft, blockEntity, angle)
        RotateShaft(phys_attacher, blockEntity, angle)

        //translateAttacher(shaft, attacher, blockEntity, facing) //TODO implement

        phys_north.renderInto(matrices, vertexConsumer)
        phys_south.renderInto(matrices, vertexConsumer)
        phys_west.renderInto(matrices, vertexConsumer)
        phys_east.renderInto(matrices, vertexConsumer)
        phys_attacher.renderInto(matrices, vertexConsumer)
        phys_shaft.renderInto(matrices, vertexConsumer)
        //RENDER CRYSTAL MATRIX start
        val interpolatedAngle = blockEntity.getInterpolatedCoreAngle(AnimationTickHolder.getPartialTicks() - 1)
        var offset = when(facing) {
            Direction.SOUTH -> org.joml.Vector3f(0.0f,0.0f,orb_offset)
            Direction.WEST -> org.joml.Vector3f(orb_offset,0.0f,0.0f)
            Direction.NORTH -> org.joml.Vector3f(0.0f,0.0f,orb_offset)
            Direction.EAST -> org.joml.Vector3f(orb_offset,0.0f,0.0f)
            Direction.UP -> org.joml.Vector3f(0.0f,orb_offset,0.0f)
            Direction.DOWN -> org.joml.Vector3f(0.0f,orb_offset,0.0f)
        }
        val innerData = TransformData(offset, org.joml.Vector3f(interpolatedAngle, interpolatedAngle, 0f))
        val data = TransformData(offset, org.joml.Vector3f(Math.toDegrees(angle.toDouble()).toFloat(), Math.toDegrees(angle.toDouble()).toFloat(), 0f))
        val outerData = TransformData(offset, org.joml.Vector3f(Math.toDegrees(angle.toDouble()).toFloat(), Math.toDegrees(angle.toDouble()).toFloat(), 0f))

        //RENDER CRYSTAL MATRIX end
        RenderUtil.renderCubeMatrix(matrices, buffer, blockState, innerData, data, outerData, 1.5f,light,overlay)

        matrices.popPose()
    }

    fun RotateShaft(partial: SuperByteBuffer, be: BlockEntity,angle: Float) {
        val facing = be.blockState.getValue(BlockStateProperties.FACING)
        if (facing == Direction.UP || facing == Direction.SOUTH || facing == Direction.EAST) {
            partial.rotateCentered(angle, Direction.Axis.Y)
        }
        else {
            partial.rotateCentered(-angle, Direction.Axis.Y)
        }
    }
    }
