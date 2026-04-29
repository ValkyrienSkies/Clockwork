package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.mod.api.dimensionId

// Interface for block entities which can generate their own connections instead of having to rely on duct
interface IConnectable: INodeBlockEntity {

    fun updateConnection(level: Level, blockPos: BlockPos, direction: Direction) {
        val neighbor = blockPos.relative(direction)
        val neighborState = level.getBlockState(neighbor) ?: return
        val neighborBlock = neighborState.block

        val selfBlock = level.getBlockState(blockPos).block as? INodeBlock ?: return

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

    fun getEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction): DuctEdge

    fun setEdge(nodeA: DuctNodePos, nodeB: DuctNodePos, level: Level, blockPos: BlockPos, direction: Direction) {
        ClockworkMod.getKelvin(level).removeEdge(nodeA, nodeB)
        ClockworkMod.getKelvin(level).addEdge(nodeA, nodeB, getEdge(nodeA, nodeB, level, blockPos, direction))
    }
}