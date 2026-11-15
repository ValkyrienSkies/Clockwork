package org.valkyrienskies.clockwork.content.physicalities.steering

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials

class SteeringWheelRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<SteeringWheelBlockEntity>(context) {
    override fun renderSafe(be: SteeringWheelBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val angle = Math.toRadians(be.renderedAngle(partialTicks).toDouble()).toFloat()
        CachedBuffers.partial(ClockworkPartials.STEERING_WHEEL, be.blockState)
            .rotateCentered(-angle, Direction.UP).light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(ms, buffers.getBuffer(RenderType.cutoutMipped()))
        CachedBuffers.block(shaft(Direction.Axis.Y)).rotateCentered(-angle, Direction.UP)
            .light<SuperByteBuffer>(light).renderInto(ms, buffers.getBuffer(RenderType.solid()))
    }
}
