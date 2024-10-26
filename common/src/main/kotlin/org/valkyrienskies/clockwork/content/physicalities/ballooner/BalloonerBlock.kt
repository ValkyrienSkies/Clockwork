package org.valkyrienskies.clockwork.content.physicalities.ballooner

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.duct.INodeBlock

class BalloonerBlock(properties: Properties): Block(properties), IBE<BalloonerBlockEntity>, INodeBlock {
    override fun getBlockEntityClass(): Class<BalloonerBlockEntity> {
        return BalloonerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out BalloonerBlockEntity> {
        return ClockworkBlockEntities.BALLOONER.get()
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        _onPlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        _onRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }
}