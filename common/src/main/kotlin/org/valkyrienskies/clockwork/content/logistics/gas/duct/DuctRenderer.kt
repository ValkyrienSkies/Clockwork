package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.kelvin.util.GasHeatLevel
import org.valkyrienskies.kelvin.util.IHeatableBlock

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

        val connection = ClockworkPartials.DUCT_CONN

        val rim = ClockworkPartials.DUCT_RIM

        val leak = ClockworkPartials.DUCT_LEAK

        val vertexConsumer = buffer.getBuffer(RenderType.cutout())

        for (dir in Direction.values()) {
            val dirConnection = CachedBufferer.partialFacing(connection, blockEntity.blockState, dir.opposite)
            val dirRim = CachedBufferer.partialFacing(rim, blockEntity.blockState, dir.opposite)
            if (blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!).isConnected) {
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
            } else if (blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!) == DuctConnectionType.LEAK) {
                val dirLeak = CachedBufferer.partialFacing(leak, blockEntity.blockState, dir.opposite)
                dirLeak.light(light).overlay(overlay).renderInto(ms, vertexConsumer)
            }
        }
    }
}
