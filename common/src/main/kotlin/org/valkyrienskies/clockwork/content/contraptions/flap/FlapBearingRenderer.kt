package org.valkyrienskies.clockwork.content.contraptions.flap

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.redstone.link.LinkRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkRenderer

class FlapBearingRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<FlapBearingBlockEntity>(context) {
    override fun renderSafe(
        te: FlapBearingBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        // if (Backend.canUseInstancing(te.getLevel())) return;
        super.renderSafe(te, partialTicks, ms, buffer, light, overlay)
        LinkRenderer.renderOnBlockEntity(te, partialTicks, ms, buffer, light, overlay)
        DualLinkRenderer.renderOnBlockEntity(te, partialTicks, ms, buffer, light, overlay)
        val bearingTe = te as IBearingBlockEntity
        val facing = te.blockState
            .getValue(BlockStateProperties.FACING)
        val top: PartialModel = ClockworkPartials.BEARING_TOP_FLAP
        val superBuffer = CachedBufferer.partial(top, te.blockState)

        val referenceFlapDir = if (facing.axis.isHorizontal) Direction.UP else Direction.NORTH
        te.targetOffset = if (te.lastFlapDir.axis != referenceFlapDir.axis) {
            if (te.lastFlapDir != facing) {
                90f
            } else {
                0f
            }
        } else {
            0f
        }

        te.currentOffset = Mth.lerp(partialTicks, te.currentOffset, te.targetOffset)
        val interpolatedAngle = bearingTe.getInterpolatedAngle(partialTicks - 1) + te.currentOffset
        kineticRotationTransform(superBuffer, te, facing.axis, (interpolatedAngle / 180f * Math.PI).toFloat(), light)
        if (facing.axis.isHorizontal)
            superBuffer.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
        superBuffer.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing)).toDouble())
        )
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))

        renderRotatingBuffer(te, getRotatedModel(te, te.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)
    }

    override fun getRotatedModel(te: FlapBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }
}
