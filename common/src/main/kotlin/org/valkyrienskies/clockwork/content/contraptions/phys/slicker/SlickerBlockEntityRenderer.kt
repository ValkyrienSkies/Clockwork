package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class SlickerBlockEntityRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<SlickerBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: SlickerBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)
    }
}