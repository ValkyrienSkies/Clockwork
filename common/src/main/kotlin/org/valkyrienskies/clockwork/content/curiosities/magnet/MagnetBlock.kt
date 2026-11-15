package org.valkyrienskies.clockwork.content.curiosities.magnet

import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkableBlock
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import kotlin.math.roundToInt

class MagnetBlock(properties: Properties, val electromagnetic: Boolean) : WrenchableDirectionalBlock(properties), IThinkableBlock {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(POWER, 0)) }

    fun fieldStrength(state: BlockState) = if (electromagnetic) state.getValue(POWER) / 15.0 * 2.0 else 1.0

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(POWER))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState = defaultBlockState()
        .setValue(FACING, if (context.isSecondaryUseActive) context.clickedFace.opposite else context.clickedFace)
        .setValue(POWER, if (electromagnetic) context.level.getBestNeighborSignal(context.clickedPos) else 0)

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, from: BlockPos, moving: Boolean) {
        if (level !is ServerLevel || !electromagnetic) return
        updatePower(state, level, pos)
    }

    private fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos) {
        val inputs = Direction.entries.mapNotNull { ThinkableConnections.input(level, pos, it) }
        val power = if (inputs.isEmpty()) level.getBestNeighborSignal(pos) else
            inputs.maxOf { (it.value?.takeIf(Double::isFinite) ?: 0.0).coerceIn(0.0, 15.0).roundToInt() }
        if (state.getValue(POWER) != power) level.setBlock(pos, state.setValue(POWER, power), 2)
        if (inputs.isNotEmpty()) level.scheduleTick(pos, this, 1)
    }

    override fun acceptsThinkable(state: BlockState, side: Direction) = electromagnetic

    override fun providesThinkable(state: BlockState, side: Direction) = false

    override fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction): Double? = null

    override fun thinkablePortName(state: BlockState, side: Direction): Component? =
        if (electromagnetic) Component.translatable("thinkable.vs_clockwork.port.magnet_power") else null

    override fun receiveThinkable(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction, value: Double?) {
        if (electromagnetic) level.scheduleTick(pos, this, 1)
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
