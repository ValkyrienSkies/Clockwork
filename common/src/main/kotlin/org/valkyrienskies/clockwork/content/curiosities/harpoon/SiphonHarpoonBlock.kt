package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.DuctNetworkUtils
import org.valkyrienskies.kelvin.api.DuctNodePos

class SiphonHarpoonBlock(properties: Properties) : HarpoonBlock(properties), ICogWheel, IDuct {
    override fun getBlockEntityType() = ClockworkBlockEntities.SIPHON_HARPOON.get()

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.clickedFace)
            .setValue(POWERED, context.level.hasNeighborSignal(context.clickedPos))

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction) = false

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter) =
        level.getBlockState(self).getValue(FACING).opposite == direction

    override fun createNode(pos: DuctNodePos) = DuctNetworkUtils.createPipeNode(pos)

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, moving: Boolean) {
        super.onPlace(state, level, pos, oldState, moving)
        nodePlace(state, level, pos, oldState, moving)
        if (!level.isClientSide)
            (level.getBlockEntity(pos) as? SiphonHarpoonBlockEntity)?.refreshConnections(level, pos)
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighbor: Block, from: BlockPos, moving: Boolean) {
        super.neighborChanged(state, level, pos, neighbor, from, moving)
        if (!level.isClientSide && from == pos.relative(state.getValue(FACING).opposite))
            (level.getBlockEntity(pos) as? SiphonHarpoonBlockEntity)?.refreshConnections(level, pos)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moving: Boolean) {
        val replaced = !state.`is`(next.block)
        if (!level.isClientSide && (replaced || state.getValue(FACING) != next.getValue(FACING))) {
            val launcher = level.getBlockEntity(pos) as? SiphonHarpoonBlockEntity
            if (launcher != null) {
                if (!replaced) launcher.release()
                val network = ClockworkMod.getKelvin(level)
                val node = launcher.getDuctNodePosition()
                network.getNodeAt(node)?.getEdges()?.toList()?.forEach { network.removeEdge(it.nodeA, it.nodeB) }
                if (replaced) network.removeNode(node)
            }
        }
        if (replaced && level.isClientSide) nodeRemoveClient(state, level, pos)
        super.onRemove(state, level, pos, next, moving)
    }
}
