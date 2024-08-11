package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock.Companion.DIR_TO_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IAxisAlignedDuct
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.NodeBehaviorType
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode

class PumpDuctBlock(properties: Properties): DirectionalKineticBlock(properties), IBE<PumpDuctBlockEntity>, INodeBlock {

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
        return PumpDuctNode(pos, NodeBehaviorType.PIPE, network, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0)
    }


    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {

        println(direction)
        if (direction.axis==level.getBlockState(self).getValue(BlockStateProperties.FACING).axis) return true
        return false
    }

    override fun getBlockEntityClass(): Class<PumpDuctBlockEntity> {
        return PumpDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PumpDuctBlockEntity> {
        return ClockworkBlockEntities.PUMP_DUCT.get()
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        _updateShape(state, direction, neighborState, level, currentPos, neighborPos)
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
    }

}