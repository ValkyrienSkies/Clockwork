package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION

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


        val leak = ClockworkPartials.DUCT_LEAK

        val vertexConsumer = buffer.getBuffer(RenderType.cutout())

        for (dir in Direction.values()) {


            if (blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!) == DuctConnectionType.LEAK) {
                val dirLeak = CachedBuffers.partialFacing(leak, blockEntity.blockState, dir.opposite)
                dirLeak.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)
            }

            if (!blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!).isConnected) continue

            val dirConnection = CachedBuffers.partialFacing(connection, blockEntity.blockState, dir.opposite)
            dirConnection.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)

            val dirBe = blockEntity.level?.getBlockEntity(blockEntity.blockPos.relative(dir))

            val edge = blockEntity.DIR_TO_CONNECTION_TYPE[dir] ?: continue

            val partial = when (edge) {
                DuctEdgeType.FILTERED -> ClockworkPartials.DUCT_SMART
                DuctEdgeType.SMART ->  ClockworkPartials.DUCT_COPPER
                DuctEdgeType.ONEWAY_FORWARD, DuctEdgeType.ONEWAY_BACKWARD ->
                    if (edge.flowsOutward(dir)) ClockworkPartials.DUCT_ONEWAY_FORWARD else ClockworkPartials.DUCT_ONEWAY_BACKWARD

                else -> if (dirBe?.blockState?.block is IDuct) null else ClockworkPartials.DUCT_RIM
            }

            if (partial != null)
                CachedBuffers.partialFacing(partial, blockEntity.blockState, dir.opposite)
                    .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)



        }




    }
}
