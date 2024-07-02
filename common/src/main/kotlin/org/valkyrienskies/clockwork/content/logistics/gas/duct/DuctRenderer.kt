package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock

class DuctRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<DuctBlockEntity>(context) {

    override fun renderSafe(
        blockEntity: DuctBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(blockEntity, partialTicks, ms, buffer, light, overlay)

        val connection = when (blockEntity.blockState.getValue(IHeatableBlock.GAS_HEAT_LEVEL)) {
            GasHeatLevel.COOL -> ClockworkPartials.DUCT_CONN_COOL
            GasHeatLevel.WARM -> ClockworkPartials.DUCT_WARM
            GasHeatLevel.HOT -> ClockworkPartials.DUCT_HOT
        }

    }
}