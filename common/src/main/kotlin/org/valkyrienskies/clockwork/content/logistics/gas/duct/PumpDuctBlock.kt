package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class PumpDuctBlock(properties: Properties): DirectionalKineticBlock(properties), IAxisAlignedDuct, ICogWheel, IBE<PumpDuctBlockEntity> {
    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return false
    }

    override fun isSmallCog(): Boolean {
        return true
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis

    }

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false))

    }
    override fun getAxis(state: BlockState?): Direction.Axis? {
        return state?.getValue(RotatedPillarBlock.AXIS)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.FOUR_VOXEL_POLE.get(state.getValue(RotatedPillarBlock.AXIS))
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)

        when (state.getValue(RotatedPillarBlock.AXIS)) {
            Direction.Axis.X -> {
                if (other != self.relative(Direction.EAST, 1) || other != self.relative(Direction.WEST, 1)) return false
            }
            Direction.Axis.Y -> {
                if (other != self.relative(Direction.UP, 1) || other != self.relative(Direction.DOWN, 1)) return false
            }
            Direction.Axis.Z -> {
                if (other != self.relative(Direction.NORTH, 1) || other != self.relative(Direction.SOUTH, 1)) return false
            }
        }

        if (level.getBlockState(other).block is IDuct) {
            if (level.getBlockState(other).block is IAxisAlignedDuct) {
                return (level.getBlockState(other).block as IAxisAlignedDuct).getAxis(level.getBlockState(other)) == state.getValue(RotatedPillarBlock.AXIS)
            }
            return true
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