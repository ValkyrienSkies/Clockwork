package org.valkyrienskies.clockwork.content.physicalities.speed_gauge

import com.jozufozu.flywheel.backend.Backend
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.content.kinetics.gauge.GaugeBlock
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import org.valkyrienskies.mod.common.getShipManagingPos

class SpeedGaugeRenderer(context: BlockEntityRendererProvider.Context?): SmartBlockEntityRenderer<SpeedGaugeBlockEntity>(context) {

    override fun renderSafe(
        be: SpeedGaugeBlockEntity,
        partialTicks: Float,
        ms: PoseStack?,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {


        val dialBuffer = CachedBufferer.partial(AllPartialModels.GAUGE_DIAL, be.blockState)

        val dialPivot = 5.75f / 16

        val facing = be.blockState.getValue(HorizontalDirectionalBlock.FACING)

        val vb = buffer.getBuffer(RenderType.solid())
        rotateBufferTowards(dialBuffer, facing).translate(0.0, dialPivot.toDouble(), dialPivot.toDouble())
            .rotate(Direction.EAST, (Math.PI / 2 * -be.current).toFloat())
            .translate(0.0, -dialPivot.toDouble(), -dialPivot.toDouble())
            .light(light)
            .renderInto(ms, vb)
    }


    fun rotateBufferTowards(buffer: SuperByteBuffer, target: Direction): SuperByteBuffer {
        return buffer.rotateCentered(Direction.UP, ((-target.toYRot() - 90) / 180 * Math.PI).toFloat())
    }


}