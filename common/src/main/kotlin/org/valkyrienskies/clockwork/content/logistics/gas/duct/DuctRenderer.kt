package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
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

//        val core = when (blockEntity.blockState.getValue(IHeatableBlock.GAS_HEAT_LEVEL)) {
//            GasHeatLevel.COOL -> ClockworkPartials.DUCT_CORE
//            GasHeatLevel.WARM -> ClockworkPartials.DUCT_CORE_WARM
//            GasHeatLevel.HOT -> ClockworkPartials.DUCT_CORE_HOT
//            GasHeatLevel.VERY_HOT -> ClockworkPartials.DUCT_CORE_VERY_HOT
//            GasHeatLevel.SUPER_HOT -> ClockworkPartials.DUCT_CORE_SUPER_HOT
//            GasHeatLevel.MOLTEN -> ClockworkPartials.DUCT_CORE_MOLTEN
//            null -> ClockworkPartials.DUCT_CORE
//        }

        val connection = when (blockEntity.blockState.getValue(IHeatableBlock.GAS_HEAT_LEVEL)) {
            GasHeatLevel.COOL -> ClockworkPartials.DUCT_CONN
            GasHeatLevel.WARM -> ClockworkPartials.DUCT_CONN_WARM
            GasHeatLevel.HOT -> ClockworkPartials.DUCT_CONN_HOT
            GasHeatLevel.VERY_HOT -> ClockworkPartials.DUCT_CONN_VERY_HOT
            GasHeatLevel.SUPER_HOT -> ClockworkPartials.DUCT_CONN_SUPER_HOT
            GasHeatLevel.MOLTEN -> ClockworkPartials.DUCT_CONN_MOLTEN
            null -> ClockworkPartials.DUCT_CONN
        }

        val rim = when (blockEntity.blockState.getValue(IHeatableBlock.GAS_HEAT_LEVEL)) {
            GasHeatLevel.COOL -> ClockworkPartials.DUCT_RIM
            GasHeatLevel.WARM -> ClockworkPartials.DUCT_RIM_WARM
            GasHeatLevel.HOT -> ClockworkPartials.DUCT_RIM_HOT
            GasHeatLevel.VERY_HOT -> ClockworkPartials.DUCT_RIM_VERY_HOT
            GasHeatLevel.SUPER_HOT -> ClockworkPartials.DUCT_RIM_SUPER_HOT
            GasHeatLevel.MOLTEN -> ClockworkPartials.DUCT_RIM_MOLTEN
            null -> ClockworkPartials.DUCT_RIM
        }

        val vertexConsumer = buffer.getBuffer(RenderType.cutout())

//        CachedBufferer.partial(core, blockEntity.blockState).light(light).overlay(overlay).renderInto(ms, vertexConsumer)

        for (dir in Direction.values()) {
            val dirConnection = CachedBufferer.partialFacing(connection, blockEntity.blockState, dir.opposite)
            val dirRim = CachedBufferer.partialFacing(rim, blockEntity.blockState, dir.opposite)
            if (blockEntity.blockState.getValue((blockEntity.blockState.block as DuctBlock).DIR_TO_CONNECTION[dir]!!).isConnected) {
                if (blockEntity.level != null) {
                    when (blockEntity.level!!.getBlockState(blockEntity.blockPos.relative(dir)).block) {
                        is DuctBlock, is IAxisAlignedDuct -> {
                            dirConnection.light(light).overlay(overlay).renderInto(ms, vertexConsumer)
                        }
                        else -> {
                            dirConnection.light(light).overlay(overlay).renderInto(ms, vertexConsumer)
                            dirRim.light(light).overlay(overlay).renderInto(ms, vertexConsumer)
                        }
                    }
                }
            }
        }
    }
}