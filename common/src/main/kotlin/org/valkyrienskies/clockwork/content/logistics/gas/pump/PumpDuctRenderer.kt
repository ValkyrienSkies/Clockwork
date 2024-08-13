package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.fluids.pump.PumpBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPartials

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

    override fun getRotatedModel(be: PumpDuctBlockEntity?, state: BlockState?): SuperByteBuffer {
        return CachedBufferer.partialFacing(ClockworkPartials.PUMP_COG, state)
    }
}