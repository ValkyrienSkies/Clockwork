package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class DeliveryChuteRenderer(context: BlockEntityRendererProvider.Context?): SmartBlockEntityRenderer<DeliveryChuteBlockEntity>(
    context
) {

    override fun renderSafe(
        blockEntity: DeliveryChuteBlockEntity?,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource?,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        if (ms==null) return
        ChuteSlotRenderer.renderOnBlockEntity(blockEntity, partialTicks, ms, buffer, light, overlay)

    }
}