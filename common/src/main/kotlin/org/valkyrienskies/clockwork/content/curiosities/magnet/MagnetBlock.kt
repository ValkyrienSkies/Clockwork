package org.valkyrienskies.clockwork.content.curiosities.magnet

import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties

class MagnetBlock(properties: Properties, val electromagnetic: Boolean) : WrenchableDirectionalBlock(properties) {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(POWER, 0)) }

    fun fieldStrength(state: BlockState) = if (electromagnetic) state.getValue(POWER) / 15.0 * 2.0 else 1.0

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(POWER))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState = defaultBlockState()
        .setValue(FACING, context.clickedFace)
        .setValue(POWER, if (electromagnetic) context.level.getBestNeighborSignal(context.clickedPos) else 0)

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, from: BlockPos, moving: Boolean) {
        if (level.isClientSide || !electromagnetic) return
        val power = level.getBestNeighborSignal(pos)
        if (state.getValue(POWER) != power) level.setBlock(pos, state.setValue(POWER, power), 2)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, old: BlockState, moving: Boolean) {
        super.onPlace(state, level, pos, old, moving)
        if (!level.isClientSide && electromagnetic && !old.`is`(this)) level.scheduleTick(pos, this, 1)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        neighborChanged(state, level, pos, this, pos, false)
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: RandomSource) {
        if (electromagnetic && state.getValue(POWER) > 0) PolaritronSound.start(pos)
    }

    override fun getRotatedBlockState(state: BlockState, face: Direction): BlockState =
        if (state.getValue(FACING).axis == face.axis) state.setValue(FACING, state.getValue(FACING).opposite)
        else super.getRotatedBlockState(state, face)

    companion object { val POWER = BlockStateProperties.POWER }
}
