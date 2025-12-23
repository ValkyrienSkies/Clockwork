package org.valkyrienskies.clockwork.content.contraptions.flap

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import dev.engine_room.flywheel.api.visualization.VisualizationManager
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
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
        be: FlapBearingBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val facing = be.blockState.getValue(BlockStateProperties.FACING)
        val axisAlong = be.blockState.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
        val interpolatedAngle = be.getInterpolatedAngle(partialTicks)

        val top: PartialModel = ClockworkPartials.BEARING_TOP_FLAP
        val superBuffer = CachedBuffers.partial(top, be.blockState)



        kineticRotationTransform(superBuffer, be, facing.axis, AngleHelper.rad(interpolatedAngle.toDouble()), light)
        if (facing.axis.isHorizontal)
            superBuffer.rotateCentered(
                AngleHelper.rad(AngleHelper.horizontalAngle(facing.opposite).toDouble()),
                Direction.UP,)
        else if (!axisAlong) superBuffer.rotateCentered(
            AngleHelper.rad(90.0),
            Direction.UP)


        superBuffer.rotateCentered(
            AngleHelper.rad((-90.0 - AngleHelper.verticalAngle(facing))),
            Direction.EAST)
        superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()))

        renderRotatingBuffer(be, getRotatedModel(be, be.blockState), ms,
            buffer.getBuffer(RenderType.solid()), light)



        if (be !is SmartFlapBearingBlockEntity) return
        DualLinkRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay)
    }

    override fun getRotatedModel(te: FlapBearingBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBuffers.partialFacing(
            AllPartialModels.SHAFT_HALF, state, state.getValue(BearingBlock.FACING).opposite
        )
    }
}
