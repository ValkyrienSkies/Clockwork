package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.configureMetalDuctEdge
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.mod.api.dimensionId

// Interface for block entities which can generate their own connections instead of having to rely on duct
interface IConnectable: INodeBlockEntity {

    /** Rebuilds direct edges after this node is restored from a loaded chunk. */
    fun refreshConnections(level: Level, blockPos: BlockPos) {
        Direction.entries.forEach { updateConnection(level, blockPos, it) }
    }

    fun updateConnection(level: Level, blockPos: BlockPos, direction: Direction) {
        val neighbor = blockPos.relative(direction)
        val neighborState = level.getBlockState(neighbor) ?: return
        val neighborBlock = neighborState.block

        val selfBlock = level.getBlockState(blockPos).block as? INodeBlock ?: return

        if (neighborBlock is DuctBlock) {
            if (!neighborBlock.canConnectTo(neighbor, blockPos, direction.opposite, level)) return
            if (!selfBlock.canConnectTo(blockPos, neighbor, direction, level)) return
            if (level.isClientSide) return

            val connectedState = neighborBlock.getConnectedState(level, neighborState, neighbor) ?: return
            if (connectedState != neighborState) level.setBlockAndUpdate(neighbor, connectedState)
            return
        }

        if (neighborBlock !is INodeBlock) return
        val neighborNodeBlock = neighborBlock as INodeBlock
        if (!neighborNodeBlock.canConnectTo(neighbor, blockPos, direction.opposite, level)) return
        if (!selfBlock.canConnectTo(blockPos, neighbor, direction, level)) return

        val neighborBe = level.getBlockEntity(neighbor) as? INodeBlockEntity
        val selfBe = level.getBlockEntity(blockPos) as? INodeBlockEntity

        val neighborDuctNodePos = neighborBe?.getDuctNodePosition() ?: DuctNodePos(
            neighbor.x.toDouble(), neighbor.y.toDouble(), neighbor.z.toDouble(),
            level.dimension().location())

        val selfDuctNodePos = selfBe?.getDuctNodePosition() ?: DuctNodePos(
            blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble(),
            level.dimension().location())

        setEdge(selfDuctNodePos, neighborDuctNodePos, level, blockPos, direction)
    }

    fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge {
        return PipeDuctEdge(ConnectionType.PIPE, nodeA, nodeB, radius = 0.3125, length = 0.375)
    }

    fun setEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction) {
        val kelvin = ClockworkMod.getKelvin(level)
        if (kelvin.getEdgeBetween(nodeA, nodeB) != null) return
        kelvin.addEdge(nodeA, nodeB, configureMetalDuctEdge(getEdge(nodeA, nodeB, level, blockPos, direction)))
    }
}
