package org.valkyrienskies.clockwork.content.contraptions.propeller

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
        ms.mulPose(Quaternion.fromXYZ(0.0f, Math.toRadians(-180.0).toFloat(), 0.0f))
        val ogfacing = te.blockState.getValue(BlockStateProperties.FACING)
        var disgustingFix = 1
        when (ogfacing) {
            Direction.SOUTH -> ms.mulPose(Vector3f.XP.rotationDegrees(270f))
            Direction.WEST -> {
                ms.mulPose(Vector3f.ZP.rotationDegrees(270f))
                disgustingFix = -disgustingFix
            }
            Direction.NORTH -> {
                ms.mulPose(Vector3f.XP.rotationDegrees(90f))
                disgustingFix = -disgustingFix
            }
            Direction.EAST -> ms.mulPose(Vector3f.ZP.rotationDegrees(90f))
            Direction.UP -> ms.mulPose(Vector3f.XP.rotationDegrees(0f))
            Direction.DOWN -> {
                ms.mulPose(Vector3f.XN.rotationDegrees(180f))
                disgustingFix = -disgustingFix
            }
        }
        ms.translate(-0.5, -0.5, -0.5)
        val interpolatedAngle: Float = bearingTe.getInterpolatedAngle(partialTicks - 1)

        kineticRotationTransform(
            superBuffer,
            te,
            Direction.UP.axis,
            disgustingFix * (interpolatedAngle / 180 * Math.PI).toFloat(),
            light
        )

        if (facing.axis.isHorizontal) superBuffer.rotateCentered(
            Direction.UP, AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        superBuffer.rotateCentered(
            Direction.UP,
            AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
        )
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))


        ms.popPose()
    }

    override fun getRotatedModel(te: PropellerBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }
}