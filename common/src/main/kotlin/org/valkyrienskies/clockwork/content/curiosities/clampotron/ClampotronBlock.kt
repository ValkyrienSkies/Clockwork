package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class ClampotronBlock(properties: Properties) : DirectionalKineticBlock(properties), IBE<ClampotronBlockEntity> {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP)) }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.clickedFace)

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    override fun getBlockEntityClass() = ClampotronBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.CLAMPOTRON.get()
    override fun getRotationAxis(state: BlockState) = state.getValue(FACING).axis
    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction) =
        face == state.getValue(FACING).opposite
}
