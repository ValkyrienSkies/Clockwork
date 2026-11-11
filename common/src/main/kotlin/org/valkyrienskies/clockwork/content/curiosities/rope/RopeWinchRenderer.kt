package org.valkyrienskies.clockwork.content.curiosities.rope

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials
import kotlin.math.PI

class RopeWinchRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<RopeWinchBlockEntity>(context) {
    override fun getRenderedBlockState(be: RopeWinchBlockEntity) =
        shaft(be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))

    override fun shouldRenderOffScreen(be: RopeWinchBlockEntity) = true
    override fun getViewDistance() = 192

    override fun renderSafe(be: RopeWinchBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        renderRotatingKineticBlock(be, getRenderedBlockState(be), ms, buffers.getBuffer(RenderType.solid()), light)
        val link = RopeKnotRenderer.winchLink(be.blockPos)
        val axis = be.blockState.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)
        val angle = if (link != null) (link.length * 3.2 % (2 * PI)).toFloat()
            else getAngleForBe(be, be.blockPos, axis) * if (axis == Direction.Axis.Z) -1f else 1f
        ms.pushPose()
        if (axis == Direction.Axis.Z) {
            ms.translate(0.5, 0.5, 0.5)
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f))
            ms.translate(-0.5, -0.5, -0.5)
        }
        val buffer = buffers.getBuffer(RenderType.cutoutMipped())
        CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_DRUM, be.blockState)
            .rotateCentered(angle, Direction.EAST).light<SuperByteBuffer>(light)
            .overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
        if (link != null) {
            CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_ROPE, be.blockState)
                .rotateCentered(angle, Direction.EAST).light<SuperByteBuffer>(light)
                .overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
            CachedBuffers.partial(ClockworkPartials.ROPE_WINCH_LEAD, be.blockState)
                .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, buffer)
        }
        ms.popPose()
    }
}
