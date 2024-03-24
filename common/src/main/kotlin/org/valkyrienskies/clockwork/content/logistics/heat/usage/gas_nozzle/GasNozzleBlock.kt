package org.valkyrienskies.clockwork.content.logistics.heat.usage.gas_nozzle

import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class GasNozzleBlock(properties: Properties) : HorizontalKineticBlock(properties), IBE<GasNozzleBlockEntity> {

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val preferredFacing = getPreferredHorizontalFacing(context)
        return if (preferredFacing != null && (context.player == null || !context.player!!.isShiftKeyDown)) withDirection(
            preferredFacing
        ) else withDirection(context.horizontalDirection.opposite)
    }

    private fun withDirection(direction: Direction): BlockState {
        return defaultBlockState().setValue(HORIZONTAL_FACING, direction)
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        if (state.getValue(FACING) == Direction.NORTH || state.getValue(FACING) == Direction.SOUTH) {
            return face == Direction.EAST || face == Direction.WEST
        }
        return face == Direction.NORTH || face == Direction.SOUTH
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return if (state.getValue(FACING).equals(Direction.NORTH) || state.getValue(FACING).equals(Direction.SOUTH)) Direction.Axis.X else Direction.Axis.Z
    }

    override fun getBlockEntityClass(): Class<GasNozzleBlockEntity> {
        return GasNozzleBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasNozzleBlockEntity> {
        return ClockworkBlockEntities.GAS_NOZZLE.get()
    }
}