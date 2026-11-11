package org.valkyrienskies.clockwork.content.curiosities.rope

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.AllSpriteShifts
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction

class RopeWinchRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<RopeWinchBlockEntity>(context) {
    override fun getRenderedBlockState(be: RopeWinchBlockEntity) =
        shaft(be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))

    override fun shouldRenderOffScreen(be: RopeWinchBlockEntity) = true
    override fun getViewDistance() = 192

    override fun renderSafe(be: RopeWinchBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        renderRotatingKineticBlock(be, getRenderedBlockState(be), ms, buffers.getBuffer(RenderType.solid()), light)
        val level = be.level as? ClientLevel ?: return
        val link = RopeKnotRenderer.winchLink(be.blockPos) ?: return
        val axis = be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)
        AbstractPulleyRenderer.scrollCoil(CachedBuffers.partialFacing(AllPartialModels.ROPE_COIL, be.blockState,
            Direction.get(Direction.AxisDirection.POSITIVE, axis)), AllSpriteShifts.ROPE_PULLEY_COIL, link.length.toFloat(), 1f)
            .light<net.createmod.catnip.render.SuperByteBuffer>(light).renderInto(ms, buffers.getBuffer(RenderType.solid()))
        CachedBuffers.partial(AllPartialModels.ROPE_HALF, be.blockState).light<net.createmod.catnip.render.SuperByteBuffer>(light)
            .renderInto(ms, buffers.getBuffer(RenderType.solid()))
    }
}
