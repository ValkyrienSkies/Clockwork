package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider

class PumpDuctRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<PumpDuctBlockEntity>(
    context
) {
    override fun renderSafe(
        be: PumpDuctBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }
}