package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.level.block.state.BlockState

class PhysicalPistonRenderer(context: BlockEntityRendererProvider.Context) :
    KineticBlockEntityRenderer<PhysicalPistonBlockEntity>(context) {

    override fun getRenderedBlockState(be: PhysicalPistonBlockEntity): BlockState = shaft(getRotationAxisOf(be))
}
