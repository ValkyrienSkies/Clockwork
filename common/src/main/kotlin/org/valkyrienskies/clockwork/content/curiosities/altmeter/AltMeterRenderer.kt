package org.valkyrienskies.clockwork.content.curiosities.altmeter

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.createmod.catnip.theme.Color
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import org.valkyrienskies.clockwork.ClockworkPartials

class AltMeterRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<AltMeterBlockEntity>(
    context
) {
    override fun renderSafe(
        blockEntity: AltMeterBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)
        val indicator = CachedBuffers.partial(ClockworkPartials.ALTIMETER_REDSTONE, blockEntity.blockState)
        val color = Color.mixColors(0x2C0300, 0xCD0000, blockEntity.signalStrength / 15f)

        indicator.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay)
            .color<SuperByteBuffer>(color)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()))
    }
}
