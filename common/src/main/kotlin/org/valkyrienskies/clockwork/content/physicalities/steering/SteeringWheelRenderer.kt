package org.valkyrienskies.clockwork.content.physicalities.steering

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import org.valkyrienskies.clockwork.ClockworkPartials

class SteeringWheelRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<SteeringWheelBlockEntity>(context) {
    override fun renderSafe(be: SteeringWheelBlockEntity, partialTicks: Float, ms: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val angle = Math.toRadians(be.renderedAngle(partialTicks).toDouble()).toFloat()
        val facing = be.blockState.getValue(HORIZONTAL_FACING)
        ms.pushPose()
        ms.translate(.5, .5, .5)
        ms.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()))
        ms.translate(0.0, -1.0 / 16.0, -5.0 / 16.0)
        ms.mulPose(Axis.XP.rotationDegrees(45f))
        ms.mulPose(Axis.ZP.rotation(angle))
        ms.translate(-.5, -6.0 / 16.0, -1.0 / 16.0)
        CachedBuffers.partial(ClockworkPartials.STEERING_WHEEL, be.blockState)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(ms, buffers.getBuffer(RenderType.cutoutMipped()))
        ms.popPose()
    }
}
