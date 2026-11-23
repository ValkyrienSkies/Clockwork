package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.DuctNodeState
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.kelvin.util.INodeBlock
import kotlin.math.min

internal object GasTransfer {
    const val MAX_PRESSURE = 16375049.0
    const val MAX_TEMPERATURE = 1478.0
    const val MIN_MASS = 1e-9

    class Port(val node: DuctNode, private val state: DuctNodeState) {
        val position get() = node.pos
        val masses = state.gasMasses.filterValues { it.isFinite() && it > 0.0 }
        val mass = masses.values.sum()
        val capacity = GasPhysics.nodeHeatCapacity(masses, node.heatCapacity)
        val temperature = if (capacity > 0.0) state.heatEnergy / capacity else state.temperature
        private val pressureVolume = state.volume * ((node as? TankDuctNode)?.size ?: 1.0)
        private val massTimesR = mass * GasPhysics.mixtureR(masses)
        val pressure = massTimesR * temperature / pressureVolume

        fun reactionState() = DuctNodeState(position, temperature, pressure, masses, state.heatEnergy, state.volume)

        fun allowedFraction(
            added: Map<GasType, Double>, energy: Double, pressureLimit: Double, temperatureLimit: Double,
            inletPressure: Double = Double.POSITIVE_INFINITY, inletPressureDrop: Double = 0.0,
        ): Double {
            val addedMassTimesR = added.values.sum() * GasPhysics.mixtureR(added)
            val addedCapacity = GasPhysics.mixtureCapacity(added)
            val maxPressure = min(pressureLimit, node.maxPressure)
            val maxTemperature = min(temperatureLimit, node.maxTemperature)

            fun fits(fraction: Double): Boolean {
                val nextTemperature = (state.heatEnergy + fraction * energy) / (capacity + fraction * addedCapacity)
                val nextPressure = (massTimesR + fraction * addedMassTimesR) * nextTemperature / pressureVolume
                return nextTemperature <= maxTemperature && nextPressure <= maxPressure &&
                    nextPressure <= inletPressure - fraction * inletPressureDrop
            }

            if (fits(1.0)) return 1.0
            if (!fits(0.0)) return 0.0
            var low = 0.0
            var high = 1.0
            repeat(16) {
                val mid = (low + high) * 0.5
                if (fits(mid)) low = mid else high = mid
            }
            return low
        }
    }

    fun port(network: DuctNetwork<*>, position: DuctNodePos): Port? {
        if (position in network.unloadedNodes) return null
        val node = network.getNodeAt(position) ?: return null
        val state = network.getNodeState(position) ?: return null
        if (state.volume <= 0.0 || !state.heatEnergy.isFinite()) return null
        return Port(node, state).takeIf { it.temperature.isFinite() && it.temperature > 0.0 && it.pressure.isFinite() }
    }

    fun inlet(level: ServerLevel, network: DuctNetwork<*>, position: BlockPos, side: Direction): Port? {
        val neighbor = position.relative(side)
        if (!level.hasChunkAt(neighbor)) return null
        val state = level.getBlockState(neighbor)
        val block = state.block as? INodeBlock ?: return null
        if (!block.canConnectTo(neighbor, position, side.opposite, level)) return null
        if (block is DuctBlock) {
            val connection = state.getValue(DuctBlock.DIR_TO_CONNECTION.getValue(side.opposite))
            if (connection != DuctConnectionType.SIDE) return null
        }
        val nodePos = ClockworkUtils.getDuctNodePos(neighbor, level)
        if (nodePos == ClockworkUtils.getDuctNodePos(position, level)) return null
        return port(network, nodePos)
    }

    fun transfer(
        network: DuctNetwork<*>, inlet: Port, outlet: Port, pressureLimit: Double, massLimit: Double,
        powered: Boolean,
    ) {
        if (inlet.mass <= MIN_MASS || massLimit <= MIN_MASS || outlet.pressure >= pressureLimit) return
        val fraction = min(massLimit / inlet.mass, 1.0)
        val masses = inlet.masses.mapValues { it.value * fraction }
        val energy = GasPhysics.mixtureCapacity(masses) * inlet.temperature
        val allowed = if (powered) {
            outlet.allowedFraction(masses, energy, pressureLimit, outlet.node.maxTemperature)
        } else {
            outlet.allowedFraction(masses, energy, pressureLimit, outlet.node.maxTemperature,
                inlet.pressure, inlet.pressure * fraction)
        }
        if (inlet.mass * fraction * allowed <= MIN_MASS) return
        remove(network, inlet, masses, allowed)
        add(network, outlet, masses, energy, allowed)
    }

    @Suppress("DEPRECATION")
    fun remove(network: DuctNetwork<*>, port: Port, masses: Map<GasType, Double>, fraction: Double) {
        masses.forEach { (gas, mass) -> network.modGasMass(port.position, gas, -mass * fraction) }
        network.modHeatEnergy(port.position, -GasPhysics.mixtureCapacity(masses) * port.temperature * fraction)
    }

    fun add(network: DuctNetwork<*>, port: Port, masses: Map<GasType, Double>, energy: Double, fraction: Double) {
        masses.forEach { (gas, mass) -> network.addGas(port.position, gas, mass * fraction, 0.0) }
        network.modHeatEnergy(port.position, energy * fraction)
    }
}
