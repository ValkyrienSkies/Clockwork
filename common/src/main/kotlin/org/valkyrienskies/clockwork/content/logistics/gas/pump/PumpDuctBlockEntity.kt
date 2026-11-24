package org.valkyrienskies.clockwork.content.logistics.gas.pump

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
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.DuctNetworkUtils.simulatesGas
import org.valkyrienskies.clockwork.util.kelvin.KNodeKineticBlockEntity
import org.valkyrienskies.kelvin.api.nodes.PumpDuctNode
import kotlin.math.abs
import kotlin.math.min

open class PumpDuctBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeKineticBlockEntity(typeIn, pos, state), IConnectable, IThinkable {

    private val mode get() = (blockState.block as PumpDuctBlock).mode
    protected val facing get() = blockState.getValue(BlockStateProperties.FACING)
    private val controlSide get() = if (facing.axis.isVertical) Direction.NORTH else Direction.UP
    private val speedRatio get() = abs(getSpeed()).toDouble() / 256.0
    protected val speedFraction get() = speedRatio.coerceAtMost(1.0)
    val pumpPressure: Double get() = speedRatio * maxPumpPressure
    private lateinit var pressure: GasPressureSetting

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehavioursDeferred(behaviours)
        if (mode == PumpDuctBlock.Mode.SMART) {
            pressure = GasPressureSetting(this, 1000, 12000, BlockStateProperties.FACING)
            behaviours.add(pressure)
        }
    }

    override fun acceptsThinkable(side: Direction) = mode == PumpDuctBlock.Mode.SMART && side == controlSide

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gas_pressure_target") else null

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null || mode != PumpDuctBlock.Mode.SMART || speedFraction <= 0.0) return
        val network = ClockworkMod.getKelvin(world)
        val inlet = GasTransfer.inlet(world, network, blockPos, facing.opposite) ?: return
        val outlet = GasTransfer.port(network, getDuctNodePosition()) ?: return
        val target = min(pressure.target(this, controlSide), inlet.pressure + SMART_HEAD * speedFraction)
        GasTransfer.transfer(network, inlet, outlet, target, 0.025 * speedFraction, powered = true)
    }

    override fun lazyTick() {
        super.lazyTick()
        updatePumpNode()
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        updatePumpNode()
    }

    fun updatePumpNode() {
        val level = level ?: return
        if (!level.simulatesGas) return
        val node = ClockworkMod.getKelvin(level).getNodeAt(getDuctNodePosition()) as? PumpDuctNode ?: return
        val outlet = blockPos.relative(facing)
        node.pumpTarget = ClockworkUtils.getDuctNodePos(outlet, level)
        node.pumpPressure = pumpPressure
    }


    companion object {
        const val maxPumpPressure = 2_000_000.0
        private const val SMART_HEAD = 12_000_000.0
    }

}
