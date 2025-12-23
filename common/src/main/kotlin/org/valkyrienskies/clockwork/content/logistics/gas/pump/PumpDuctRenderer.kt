package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.fluids.pump.PumpBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

class PumpDuctRenderer(context: BlockEntityRendererProvider.Context) : KineticBlockEntityRenderer<PumpDuctBlockEntity>(
    context
) {
    override fun getRotatedModel(be: PumpDuctBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBuffers.partialFacing(AllPartialModels.MECHANICAL_PUMP_COG, state)
    }
}
