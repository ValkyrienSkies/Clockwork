package org.valkyrienskies.clockwork.content.logistics.gas.duct

import com.simibubi.create.AllShapes
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.RotatedPillarBlock
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel

open class StraightDuctBlock(properties: Properties) : RotatedPillarBlock(properties), IAxisAlignedDuct, SimpleWaterloggedBlock {

    val gasHeatLevel: EnumProperty<GasHeatLevel> = EnumProperty.create("heat_level", GasHeatLevel::class.java)

    init {
        registerDefaultState(super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false).setValue(gasHeatLevel, GasHeatLevel.COOL))
    }

    override fun getAxis(state: BlockState?): Direction.Axis? {
        return state?.getValue(AXIS)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.FOUR_VOXEL_POLE.get(state.getValue(AXIS))
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)

        when (state.getValue(AXIS)) {
            Direction.Axis.X -> {
                if (other != self.relative(Direction.EAST, 1) || other != self.relative(Direction.WEST, 1)) return false
            }
            Direction.Axis.Y -> {
                if (other != self.relative(Direction.UP, 1) || other != self.relative(Direction.DOWN, 1)) return false
            }
            Direction.Axis.Z -> {
                if (other != self.relative(Direction.NORTH, 1) || other != self.relative(Direction.SOUTH, 1)) return false
            }
            else -> {}
        }

        if (level.getBlockState(other).block is IDuct) {
            return true
        }
        return false
    }

}