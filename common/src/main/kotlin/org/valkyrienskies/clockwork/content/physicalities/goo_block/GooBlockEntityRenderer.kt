package org.valkyrienskies.clockwork.content.physicalities.goo_block

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class GooBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<GooBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: GooBlockEntity?,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)
    }
}