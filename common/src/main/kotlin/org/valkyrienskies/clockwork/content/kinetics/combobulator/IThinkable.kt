package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

interface IThinkable {
    fun providesThinkable(side: Direction): Boolean = false
    fun acceptsThinkable(side: Direction): Boolean = false
    fun thinkableValue(side: Direction): Double? = null
    fun receiveThinkable(side: Direction, value: Double?) = Unit
    fun onThinkablePulse(side: Direction) = Unit
    fun thinkablePortName(side: Direction): Component? = when {
        acceptsThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.input")
        providesThinkable(side) -> Component.translatable("thinkable.vs_clockwork.port.output")
        else -> null
    }
}
