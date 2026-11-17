package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading

interface IThinkableBlock {
    fun acceptsThinkable(state: BlockState, side: Direction): Boolean = false
    fun receiveThinkable(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction, value: Double?) {}
    fun providesThinkable(state: BlockState, side: Direction): Boolean
    fun thinkableReadings(state: BlockState, side: Direction): List<ThinkableReading> = emptyList()
    fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction): Double?
    fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction, reading: String): Double? =
        if (reading.isEmpty()) thinkableValue(state, level, pos, side) else null
    fun thinkablePortName(state: BlockState, side: Direction): Component?
}
