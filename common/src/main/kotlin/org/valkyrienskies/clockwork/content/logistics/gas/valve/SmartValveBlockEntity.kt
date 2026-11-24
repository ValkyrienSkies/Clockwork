package org.valkyrienskies.clockwork.content.logistics.gas.valve

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.logistics.gas.GasPressureSetting
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity

class SmartValveBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable, IThinkable {

    private lateinit var pressure: GasPressureSetting

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        pressure = GasPressureSetting(this, 400, 12000, BlockStateProperties.HORIZONTAL_FACING)
        behaviours.add(pressure)
    }

    override fun acceptsThinkable(side: Direction) = side == Direction.UP

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gas_pressure_target") else null

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null) return
        val network = ClockworkMod.getKelvin(world)
        val facing = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING)
        val inlet = GasTransfer.inlet(world, network, blockPos, facing.opposite) ?: return
        val outlet = GasTransfer.port(network, getDuctNodePosition()) ?: return
        GasTransfer.transfer(network, inlet, outlet, pressure.target(this, Direction.UP), 0.125, powered = false)
    }
}
