package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.createEdgeType
import org.valkyrienskies.mod.common.util.toJOMLD

class PumpDuctBlock(properties: Properties): DirectionalKineticBlock(properties), IBE<PumpDuctBlockEntity>, INodeBlock,
    ICogWheel {

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.COOL))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(IHeatableBlock.GAS_HEAT_LEVEL)
        builder.add(BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }


    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(BlockStateProperties.FACING).axis
    }


    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.SIX_VOXEL_POLE.get(state.getValue(BlockStateProperties.FACING).axis)
    }

    override fun createNode(pos: DuctNodePos, network: DuctNetwork): DuctNode {
        return PumpDuctNode(pos, NodeBehaviorType.PUMP, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }


    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {

        if (direction.axis==level.getBlockState(self).getValue(BlockStateProperties.FACING).axis) return true
        return false
    }

    override fun getBlockEntityClass(): Class<PumpDuctBlockEntity> {
        return PumpDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PumpDuctBlockEntity> {
        return ClockworkBlockEntities.PUMP_DUCT.get()
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        val facing = state.getValue(BlockStateProperties.FACING)

        nodePlace(state, level, pos, oldState, isMoving)

        handleConnections(pos, facing,level)
        handleConnections(pos, facing.opposite,level)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }





    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
        {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }
        handleConnections(currentPos,direction,level)


        return state
    }




    fun handleConnections(currentPos: BlockPos, direction: Direction, level: BlockGetter) {

        val neighborPos = currentPos.relative(direction)
        val neighborState = level.getBlockState(neighborPos)

        var otherConnected = false

        val canConnect = canConnectTo(currentPos, neighborPos, direction.getOpposite(), level as Level) && level.getBlockState(neighborPos).block is IDuct

        if (neighborState.block is DuctBlock)
        {
            otherConnected = neighborState.getValue(DIR_TO_CONNECTION[direction.opposite]!!).canBeChanged()

        } else if (neighborState.block is IDuct) {
            otherConnected =  (neighborState.block as IDuct).canConnectTo(neighborPos, currentPos, direction.opposite, level)
        }




        if (otherConnected && canConnect) {
            ClockworkMod.getKelvin().removeEdge(currentPos.toJOMLD(), neighborPos.toJOMLD())

            val newEdge = createEdgeType(currentPos.toJOMLD(), neighborPos.toJOMLD(), ConnectionType.PIPE)
            ClockworkMod.getKelvin().addEdge(currentPos.toJOMLD(), neighborPos.toJOMLD(), newEdge)

        }
    }

}