package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import net.minecraft.core.BlockPos
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.kelvin.util.GasPhysics


internal class DuctTankContentTransfer(private val network: DuctNetwork<*>) {
    private data class Contents(val masses: Map<GasType, Double>, val energy: Double)
    private data class Source(val members: List<BlockPos>, val contents: Contents)

    private val sources = mutableListOf<Source>()
    private val capturedMembers = hashSetOf<BlockPos>()

    fun hasCaptured(member: BlockPos): Boolean = member in capturedMembers

    fun capture(controller: DuctNodePos, members: List<BlockPos>) {
        // Create temporarily turns members into singleton controllers while rebuilding.
        // They still belong to their original snapshot, not to new sources of gas.
        if (members.any { it in capturedMembers }) return
        capturedMembers.addAll(members)
        val info = network.nodeInfo[controller] ?: return
        sources += Source(members.map { it.immutable() }, Contents(HashMap(info.currentGasMasses), info.currentEnergy))
    }

    fun restore(destinationAt: (BlockPos) -> DuctNodePos?) {
        val masses = linkedMapOf<DuctNodePos, MutableMap<GasType, Double>>()
        val energies = hashMapOf<DuctNodePos, Double>()
        for ((members, contents) in sources) {
            val destinations = members.mapNotNull(destinationAt).groupingBy { it }.eachCount()

            for ((destination, count) in destinations) {
                val fraction = count.toDouble() / members.size
                val target = masses.getOrPut(destination) { hashMapOf() }
                for ((gas, mass) in contents.masses) {
                    target[gas] = (target[gas] ?: 0.0) + mass * fraction
                }
                energies[destination] = (energies[destination] ?: 0.0) + contents.energy * fraction
            }
        }

        for ((destination, contents) in masses) {
            val node = network.getNodeAt(destination) ?: continue
            val info = network.nodeInfo[destination] ?: continue
            info.currentGasMasses.clear()
            info.currentGasMasses.putAll(contents)
            info.currentEnergy = energies.getValue(destination)
            info.totalVolume = node.volume
            info.volumeChange = 0.0
            info.previousVolumeChange = 0.0
            val capacity = GasPhysics.nodeHeatCapacity(contents, node.heatCapacity)
            info.currentTemperature = if (capacity > 0.0) info.currentEnergy / capacity else 273.15
            info.currentPressure = GasPhysics.calcPressureFromGamma(contents, node.volume, info.currentTemperature) /
                ((node as? TankDuctNode)?.size ?: 1.0)
            info.previousPressure = info.currentPressure
        }
    }
}
