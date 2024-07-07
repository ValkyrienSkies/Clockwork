package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock

class PumpDuctBlock(properties: Properties): DirectionalKineticBlock(properties), IAxisAlignedDuct, ICogWheel, IBE<PumpDuctBlockEntity>, IHeatableBlock {

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.COOL))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(IHeatableBlock.GAS_HEAT_LEVEL)
        builder.add(BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return false
    }

    override fun isSmallCog(): Boolean {
        return true
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).clockWise.axis

    }

    override fun getAxis(state: BlockState?): Direction.Axis? {
        return state!!.getValue(FACING).axis
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.FOUR_VOXEL_POLE.get(state.getValue(FACING).axis)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)

        if (direction.axis != state.getValue(RotatedPillarBlock.AXIS)) return false

        if (level.getBlockState(other).block is IDuct) {
            if (level.getBlockState(other).block is IAxisAlignedDuct) {
                return (level.getBlockState(other).block as IAxisAlignedDuct).getAxis(level.getBlockState(other)) == state.getValue(RotatedPillarBlock.AXIS)
            }
            if (level.getBlockState(other).block is DuctBlock) {
                if (level.getBlockState(other).getValue((level.getBlockState(other).block as DuctBlock).DIR_TO_CONNECTION[direction]!!).isConnected) {
                    return true
                }
            }
        }
        return false
    }

    override fun getBlockEntityClass(): Class<PumpDuctBlockEntity> {
        return PumpDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PumpDuctBlockEntity> {
        return ClockworkBlockEntities.PUMP_DUCT.get()
    }

}