package org.valkyrienskies.clockwork.content.contraptions.flap

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AngleHelper
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

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
        val bearingTe = te as IBearingBlockEntity
        val facing = te.blockState
            .getValue(BlockStateProperties.FACING)
        val top: PartialModel = ClockworkPartials.BEARING_TOP_FLAP
        val superBuffer = CachedBufferer.partial(top, te.blockState)
        val interpolatedAngle = bearingTe.getInterpolatedAngle(partialTicks - 1)
        kineticRotationTransform(superBuffer, te, facing.axis, (interpolatedAngle / 180 * Math.PI).toFloat(), light)
        if (facing.axis.isHorizontal)
            superBuffer.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble())
            )
        superBuffer.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90 - AngleHelper.verticalAngle(facing)).toDouble())
        )
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }

    override fun getRotatedModel(te: FlapBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }
}
