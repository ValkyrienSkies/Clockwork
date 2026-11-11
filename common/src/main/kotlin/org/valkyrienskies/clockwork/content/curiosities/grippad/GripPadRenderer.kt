package org.valkyrienskies.clockwork.content.curiosities.grippad

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.simibubi.create.AllPartialModels
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.DirectionalBlock

class GripPadRenderer(context: BlockEntityRendererProvider.Context) : SafeBlockEntityRenderer<GripPadBlockEntity>() {
    override fun renderSafe(be: GripPadBlockEntity, partialTicks: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        val facing = be.blockState.getValue(DirectionalBlock.FACING)
        val compression = be.previousCompression + (be.compression - be.previousCompression) * partialTicks
        pose.pushPose()
        pose.translate(0.5, 0.5, 0.5)
        pose.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing)))
        pose.mulPose(Axis.XP.rotationDegrees(AngleHelper.verticalAngle(facing) + 90f))
        pose.translate(-0.5, -0.5, -0.5)
        pose.scale(1f, 0.5f, 1f)
        pose.translate(0.0, 3.0 / 16.0 - compression * 0.06, 0.0)
        CachedBuffers.partial(AllPartialModels.STICKER_HEAD, be.blockState)
            .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .renderInto(pose, buffers.getBuffer(RenderType.solid()))
        pose.popPose()
    }
}
