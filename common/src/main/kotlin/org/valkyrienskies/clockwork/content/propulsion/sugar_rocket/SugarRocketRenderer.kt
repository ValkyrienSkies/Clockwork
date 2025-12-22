package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.createmod.catnip.theme.Color
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class SugarRocketRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<SugarRocketBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: SugarRocketBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        val superByteBuffer = CachedBuffers.block(blockEntity.blockState)
        val color = Color.mixColors(0xFFFFFF, 0x000000, blockEntity.clientBurnProgress.getValue(partialTicks))
        if (blockEntity.isBurning) {
            superByteBuffer.light<SuperByteBuffer>(light).color<SuperByteBuffer>(color).renderInto(ms, buffer.getBuffer(RenderType.cutout()))
        } else {
            superByteBuffer.light<SuperByteBuffer>(light).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        }

    }
}
