package org.valkyrienskies.clockwork.content.logistics.gas.engine

import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

class SterlingEngineRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<SterlingEngineBlockEntity>(context) {

    override fun getRotatedModel(be: SterlingEngineBlockEntity, state: BlockState): SuperByteBuffer {
        return CachedBuffers.partialFacing(AllPartialModels.MECHANICAL_PUMP_COG, state)
    }
}
