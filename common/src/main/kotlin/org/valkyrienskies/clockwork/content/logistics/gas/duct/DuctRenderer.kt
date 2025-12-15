package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import dev.engine_room.flywheel.lib.model.baked.PartialModel
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.core.impl.shadow.ke
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.util.IEdgeBlock
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity

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

        val kelvin = ClockworkModClient.getKelvin()

        for (dir in Direction.values()) {

            if (!blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!).isConnected) continue

            val dirConnection = CachedBuffers.partialFacing(connection, blockEntity.blockState, dir.opposite)
            dirConnection.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)
            if (blockEntity.blockState.getValue(DIR_TO_CONNECTION[dir]!!) == DuctConnectionType.LEAK) {
                val dirLeak = CachedBuffers.partialFacing(leak, blockEntity.blockState, dir.opposite)
                dirLeak.light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)
            }

            val dirBe = blockEntity.level?.getBlockEntity(blockEntity.blockPos.relative(dir))

            val edge = blockEntity.DIR_TO_CONNECTION_TYPE[dir] ?: continue

            val partial = when (edge) {
                DuctEdgeType.FILTERED -> ClockworkPartials.DUCT_SMART
                DuctEdgeType.SMART ->  ClockworkPartials.DUCT_COPPER
                // Edge directionality is enforced by axis direction for oneways
                DuctEdgeType.ONEWAY_BACKWARD ->
                    if (dir.axisDirection.step == 1) ClockworkPartials.DUCT_ONEWAY_FORWARD else ClockworkPartials.DUCT_ONEWAY_BACKWARD
                DuctEdgeType.ONEWAY_FORWARD ->
                    if (dir.axisDirection.step == -1) ClockworkPartials.DUCT_ONEWAY_FORWARD else ClockworkPartials.DUCT_ONEWAY_BACKWARD

                else -> if (dirBe?.blockState?.block is IDuct) null else ClockworkPartials.DUCT_RIM
            }

            if (partial != null)
                CachedBuffers.partialFacing(partial, blockEntity.blockState, dir.opposite)
                    .light<SuperByteBuffer>(light).overlay<SuperByteBuffer>(overlay).renderInto(ms, vertexConsumer)



        }




    }
}
