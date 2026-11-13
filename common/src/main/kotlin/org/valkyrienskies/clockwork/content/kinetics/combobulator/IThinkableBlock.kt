package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState

interface IThinkableBlock {
    fun providesThinkable(state: BlockState, side: Direction): Boolean
    fun thinkableValue(state: BlockState, level: ServerLevel, pos: BlockPos, side: Direction): Double?
    fun thinkablePortName(state: BlockState, side: Direction): Component?
}
