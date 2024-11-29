package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

interface IEdgeBlock {

    fun connectedTo(pos: BlockPos): Boolean

    fun tryConnectEdge(level: Level, pos: BlockPos)

    fun tryDisconnectEdge(level: Level, pos: BlockPos)
}