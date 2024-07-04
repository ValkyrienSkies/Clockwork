package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.FrequencySlotRenderer

class DeliveryCannonRenderer(context: BlockEntityRendererProvider.Context?): FrequencySlotRenderer<DeliveryCannonBlockEntity>(context) {


    override fun renderSafe(
        be: DeliveryCannonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)

        val antenna = CachedBufferer.partial(ClockworkPartials.CANNON_ANTENNA,be.blockState)
        val base = CachedBufferer.partial(ClockworkPartials.CANNON_BASE,be.blockState)
        val mount = CachedBufferer.partial(ClockworkPartials.CANNON_MOUNT,be.blockState)
        val barrel = CachedBufferer.partial(ClockworkPartials.CANNON_BARREL,be.blockState)

        val vb = buffer.getBuffer(RenderType.cutout())

        renderBufferNeutral(be,antenna,ms,vb,light)
        renderBufferNeutral(be,base,ms,vb,light)
        renderBufferNeutral(be,mount,ms,vb,light)
        renderBufferNeutral(be,barrel,ms,vb,light)

    }

    fun rotateBufferTowards(buffer: SuperByteBuffer, target: Direction): SuperByteBuffer {
        return buffer.rotateCentered(Direction.UP, ((-target.toYRot() - 90) / 180 * Math.PI).toFloat())
    }

    fun renderBufferNeutral(be: DeliveryCannonBlockEntity, bf: SuperByteBuffer, ms: PoseStack?, vb: VertexConsumer, light: Int) {
        rotateBufferTowards(bf,be.blockState.getValue(HorizontalDirectionalBlock.FACING)).light(light).renderInto(ms,vb)
    }
}