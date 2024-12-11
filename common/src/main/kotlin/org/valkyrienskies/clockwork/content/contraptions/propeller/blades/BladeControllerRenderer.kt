package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class BladeControllerRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<BladeControllerBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: BladeControllerBlockEntity?,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource?,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)
    }
}