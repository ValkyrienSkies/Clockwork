package org.valkyrienskies.clockwork.content.propulsion.sugar_rocket

import com.jozufozu.flywheel.util.Color
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
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

        if (blockEntity.isBurning) {
            val superByteBuffer = CachedBufferer.block(blockEntity.blockState)
            val color = Color.mixColors(0xFFFFFF, 0x000000, blockEntity.clientBurnProgress.getValue(partialTicks))
            superByteBuffer.light(15).overlay(overlay).color(color).renderInto(ms, buffer.getBuffer(RenderType.solid()))
        }
    }
}