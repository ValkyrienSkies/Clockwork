package org.valkyrienskies.clockwork.content.contraptions.flap

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
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
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.smart_flap.SmartFlapBearingBlockEntity

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

        val facing = te.blockState.getValue(BlockStateProperties.FACING)
        val axisAlong = te.blockState.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        val interpolatedAngle = te.getInterpolatedAngle(partialTicks)

        val top: PartialModel = ClockworkPartials.BEARING_TOP_FLAP
        val superBuffer = CachedBufferer.partial(top, te.blockState)



        kineticRotationTransform(superBuffer, te, facing.axis, AngleHelper.rad(interpolatedAngle.toDouble()), light)
        if (facing.axis.isHorizontal)
            superBuffer.rotateCentered(
                Direction.UP,
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()))
        else if (!axisAlong) superBuffer.rotateCentered(
            Direction.UP,
            AngleHelper.rad(90.0))


        superBuffer.rotateCentered(
            Direction.EAST,
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing))))
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))

        renderRotatingBuffer(te, getRotatedModel(te, te.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)



        if (te !is SmartFlapBearingBlockEntity) return
        DualLinkRenderer.renderOnBlockEntity(te, partialTicks, ms, buffer, light, overlay)
    }

    override fun getRotatedModel(te: FlapBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBufferer.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }
}
