package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos

interface IEdgeBlock {

    fun connectedTo(pos: BlockPos): Boolean
}