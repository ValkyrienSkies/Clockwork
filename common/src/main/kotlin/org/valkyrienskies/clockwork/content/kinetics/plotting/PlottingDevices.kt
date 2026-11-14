package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.api.thinkable.Thinkable
import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.compat.thinkable.ThinkableCompat
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkableBlock

internal data class PlottingPort(val side: Direction, val reading: String, val name: Component) {
    fun label(): Component = Component.translatable("gui.vs_clockwork.plotting.port", name,
        Component.translatable("gui.vs_clockwork.plotting.side.${side.serializedName}"))
}

internal data class PlottingDevice(val id: String, val state: BlockState, val outputs: List<PlottingPort>, val inputs: List<PlottingPort>) {
    val name: Component get() = state.block.name
    fun ports(kind: PlottingKind) = if (kind == PlottingKind.OUTPUT) inputs else outputs
}

internal object PlottingDevices {
    fun discover(level: Level): List<PlottingDevice> = BuiltInRegistries.BLOCK.mapNotNull { block ->
        val state = block.defaultBlockState()
        if (block !is IThinkableBlock && block !is EntityBlock && !ThinkableCompat.supports(state)) return@mapNotNull null
        try {
            val entity = (block as? EntityBlock)?.newBlockEntity(BlockPos.ZERO, state)
            val context = ThinkableContext(level, BlockPos.ZERO, state, entity)
            val device = ThinkableCompat.find(context) ?: entity as? Thinkable ?: (block as? IThinkableBlock)?.let { native ->
                object : Thinkable {
                    override fun providesThinkable(side: Direction) = native.providesThinkable(state, side)
                    override fun thinkablePortName(side: Direction) = native.thinkablePortName(state, side)
                }
            } ?: return@mapNotNull null
            describe(state, device)
        } catch (e: Exception) {
            ClockworkMod.LOGGER.debug("Cannot preview Thinkable {}", BuiltInRegistries.BLOCK.getKey(block), e)
            null
        }
    }.sortedWith(compareBy({ it.name.string }, { it.id }))

    fun describe(state: BlockState, device: Thinkable): PlottingDevice? {
        val outputs = mutableListOf<PlottingPort>()
        val inputs = mutableListOf<PlottingPort>()
        for (side in Direction.entries) {
            if (device.providesThinkable(side)) {
                val readings = device.thinkableReadings(side).filter { it.id().length <= 128 }.distinctBy { it.id() }.take(32)
                if (readings.isEmpty()) outputs.add(PlottingPort(side, "", device.thinkablePortName(side)
                    ?: Component.translatable("thinkable.vs_clockwork.port.output")))
                else readings.forEach { outputs.add(PlottingPort(side, it.id(), it.name())) }
            }
            if (device.acceptsThinkable(side)) inputs.add(PlottingPort(side, "", device.thinkablePortName(side)
                ?: Component.translatable("thinkable.vs_clockwork.port.input")))
        }
        return if (outputs.isEmpty() && inputs.isEmpty()) null
        else PlottingDevice(BuiltInRegistries.BLOCK.getKey(state.block).toString(), state, outputs, inputs)
    }
}
