package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.api.thinkable.Thinkable

class ThinkableRelayBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), IThinkable {

    val inputSide: Direction get() = blockState.getValue(BlockStateProperties.FACING)

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit
    override fun acceptsThinkable(side: Direction) = side == inputSide
    override fun providesThinkable(side: Direction) = side != inputSide
    override fun thinkablePortName(side: Direction): Component = Component.translatable(
        if (acceptsThinkable(side)) "thinkable.vs_clockwork.port.relay_input" else "thinkable.vs_clockwork.port.relay_output")

    override fun thinkableValue(side: Direction) = thinkableValue(side, "")

    override fun thinkableValue(side: Direction, reading: String): Double? {
        val source = source(side) ?: return null
        return source.device?.thinkableValue(source.side, reading)?.takeIf { it.isFinite() }
    }

    override fun thinkableReadings(side: Direction) = source(side)?.let {
        it.device?.thinkableReadings(it.side)
    } ?: emptyList()

    private fun source(side: Direction): ThinkableConnections.Source? {
        var device: Thinkable = this
        var output = side
        val visited = mutableSetOf<ThinkableRelayBlockEntity>()
        while (device is ThinkableRelayBlockEntity) {
            if (!device.providesThinkable(output) || !visited.add(device)) return null
            val source = ThinkableConnections.inputSource(device.level, device.blockPos, device.inputSide) ?: return null
            device = source.device ?: return null
            output = source.side
        }
        return ThinkableConnections.Source(device, output)
    }

    override fun tick() {
        super.tick()
        if (level is ServerLevel) sendOutputs(thinkableValue(inputSide.opposite), inputSide)
    }

    fun clearOutputs(input: Direction = inputSide) = sendOutputs(null, input, clear = true)

    private fun sendOutputs(value: Double?, input: Direction, clear: Boolean = false) {
        val world = level as? ServerLevel ?: return
        for (side in Direction.entries) {
            if (side == input) continue
            ThinkableConnections.sendAdjacent(world, blockPos, side, value, clear)
        }
    }
}
