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
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor

class ThinkableRelayBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(type, pos, state), IThinkable {

    val inputSide: Direction get() = blockState.getValue(BlockStateProperties.FACING)

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit
    override fun acceptsThinkable(side: Direction) = side == inputSide
    override fun providesThinkable(side: Direction) = side != inputSide
    override fun thinkablePortName(side: Direction): Component = Component.translatable(
        if (acceptsThinkable(side)) "thinkable.vs_clockwork.port.relay_input" else "thinkable.vs_clockwork.port.relay_output")

    override fun thinkableValue(side: Direction): Double? {
        var device: IThinkable = this
        var output = side
        val visited = mutableSetOf<ThinkableRelayBlockEntity>()
        while (device is ThinkableRelayBlockEntity) {
            if (!device.providesThinkable(output) || !visited.add(device)) return null
            val source = ThinkableConnections.inputSource(device.level, device.blockPos, device.inputSide) ?: return null
            device = source.device ?: return null
            output = source.side
        }
        return device.thinkableValue(output)?.takeIf { it.isFinite() }
    }

    override fun tick() {
        super.tick()
        if (level is ServerLevel) sendOutputs(thinkableValue(inputSide.opposite), inputSide)
    }

    fun clearOutputs(input: Direction = inputSide) = sendOutputs(null, input)

    private fun sendOutputs(value: Double?, input: Direction) {
        val world = level as? ServerLevel ?: return
        for (side in Direction.entries) {
            if (side == input) continue
            val sink = WandAnchor.at(world, blockPos.relative(side), side.opposite)
            if (ThinkableConnections.incoming(world, sink) != null) continue
            val receiver = ThinkableConnections.device(world, sink) ?: continue
            if (receiver.acceptsThinkable(sink.face)) receiver.receiveThinkable(sink.face, value)
        }
    }
}
