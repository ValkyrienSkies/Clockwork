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

class ThrottleLeverRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<ThrottleLeverBlockEntity>(context) {
    override fun renderSafe(
        be: ThrottleLeverBlockEntity, partialTicks: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int,
    ) {
        val facing = be.blockState.getValue(HORIZONTAL_FACING)
        pose.pushPose()
        pose.translate(.5, .25, .5)
        pose.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()))
        pose.mulPose(Axis.XP.rotationDegrees(be.renderedAngle(partialTicks)))
        pose.translate(-.5, -.25, -.5)
        CachedBuffers.partial(ClockworkPartials.THROTTLE_LEVER_HANDLE, be.blockState)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(pose, buffers.getBuffer(RenderType.cutoutMipped()))
        pose.popPose()
    }
}
