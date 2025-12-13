package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.curiosities.sensor.ISensorBlock.Companion.POWER
import org.valkyrienskies.clockwork.content.curiosities.sensor.distance.DistanceSensorBlock.Companion.MAX_DISTANCE
import org.valkyrienskies.kelvin.util.INodeBlock

class GasCrafterBlock(properties: Properties) : DirectionalBlock(properties), IBE<GasCrafterBlockEntity>, INodeBlock {
    override fun getBlockEntityClass(): Class<GasCrafterBlockEntity> {
        return GasCrafterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasCrafterBlockEntity?>? {
        return ClockworkBlockEntities.GAS_CRAFTER.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        super.onPlace(state, level, pos, oldState, movedByPiston)
        nodePlace(state, level, pos, oldState, movedByPiston)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, movedByPiston: Boolean) {
        nodeRemove(state, level, pos, newState, movedByPiston)
        super.onRemove(state, level, pos, newState, movedByPiston)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, POWER, MAX_DISTANCE)
    }

    init {
        this.registerDefaultState(defaultBlockState()!!.setValue(FACING, Direction.NORTH))
    }

    override fun rotate(state: BlockState, rotation: Rotation): BlockState {
        return state.setValue(
            FACING,
            rotation.rotate(state.getValue(FACING))
        ) as BlockState
    }

    override fun mirror(state: BlockState, mirror: Mirror): BlockState {
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return this.defaultBlockState()
            .setValue<Direction, Direction>(DirectionalBlock.FACING, context.nearestLookingDirection.opposite.opposite)
    }
}