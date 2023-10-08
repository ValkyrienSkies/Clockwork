package org.valkyrienskies.clockwork.content.contraptions.phys.altmeter

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class AltMeterBlock(properties: Properties): Block(properties), IBE<AltMeterBlockEntity> {
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        for (direction in Direction.entries) {
            level.updateNeighborsAt(pos.relative(direction), this)
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (isMoving) {
            return
        }
        for (direction in Direction.entries) {
            level.updateNeighborsAt(pos.relative(direction), this)
        }
    }

    override fun getSignal(state: BlockState, level: BlockGetter, pos: BlockPos, direction: Direction): Int {
        return if (state.getValue(POWERED)) {
            15
        } else 0
    }

    override fun isSignalSource(state: BlockState): Boolean {
        return true
    }

    override fun getBlockEntityClass(): Class<AltMeterBlockEntity> {
        return AltMeterBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out AltMeterBlockEntity> {
        return ClockworkBlockEntities.ALT_METER.get()
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(POWERED)
    }

    companion object {
        val POWERED: BooleanProperty = BlockStateProperties.POWERED
    }
}
