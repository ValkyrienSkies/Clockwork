package org.valkyrienskies.clockwork.kelvin.impl

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Explosion
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.edges.ApertureEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.OneWayEdge
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min
import kotlin.math.pow

class DuctNetworkImpl(
    override val nodes: HashMap<DuctNodePos, DuctNode> = hashMapOf(),
    override val edges: HashSet<DuctEdge> = hashSetOf(),
    override val nodeInfo: HashMap<DuctNodePos, DuctNodeInfo> = hashMapOf(),
    override val unloadedNodes: HashSet<DuctNodePos> = hashSetOf()
) : DuctNetwork {

    override fun markLoaded(pos: DuctNodePos) {
        if (!nodes.contains(pos)) {
            return
        }
        unloadedNodes.remove(pos)
    }

    override fun markUnloaded(pos: DuctNodePos) {
        if (!nodes.contains(pos)) {
            return
        }
        unloadedNodes.add(pos)
    }

    override fun getFlowBetween(from: DuctNodePos, to: DuctNodePos): Double {
        val edge = getEdgeBetween(from, to) ?: return 0.0
        return edge.currentFlowRate
    }

    override fun getPressureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentPressure ?: 0.0
    }

    override fun getTemperatureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentTemperature ?: 0.0
    }

    override fun getGasVolumesAt(node: DuctNodePos): EnumMap<GasType, Double> {
        return nodeInfo[node]?.currentGasMasses ?: EnumMap(GasType::class.java)
    }

    override fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge? {
        return edges.find { (it.nodeA == from && it.nodeB == to) || (it.nodeA == to && it.nodeB == from) }
    }

    override fun getNodeAt(pos: DuctNodePos): DuctNode? {
        return nodes[pos]
    }

    override fun addNode(pos: DuctNodePos, node: DuctNode) {
        nodes[pos] = node
        nodeInfo[pos] = DuctNodeInfo(node.behavior, 0.0, 0.0, EnumMap<GasType, Double>(GasType::class.java))
    }

    override fun removeNode(pos: DuctNodePos) {
        nodes.remove(pos)
        nodeInfo.remove(pos)
        val edgesToRemove = edges.filter { it.nodeA == pos || it.nodeB == pos }
        edges.removeAll(edgesToRemove.toSet())

        if (unloadedNodes.contains(pos)) {
            unloadedNodes.remove(pos)
        }
    }

    override fun addEdge(posA: DuctNodePos, posB: DuctNodePos, edge: DuctEdge) {
        edges.find { (it.nodeA == posA && it.nodeB == posB) || (it.nodeA == posB && it.nodeB == posA) }?.let {
            return
        }
        if (posA == posB) {
            return
        }
        edges.add(edge)
        nodes[posA]?.nodeEdges?.add(edge)
        nodes[posB]?.nodeEdges?.add(edge)
    }

    override fun removeEdge(posA: DuctNodePos, posB: DuctNodePos) {
        edges.find { (it.nodeA == posA && it.nodeB == posB) || (it.nodeA == posB && it.nodeB == posA) }?.let {
            edges.remove(it)
            nodes[posA]?.nodeEdges?.remove(it)
            nodes[posB]?.nodeEdges?.remove(it)
        }
    }

    override fun modTemperature(pos: DuctNodePos, deltaTemperature: Double) {
        nodeInfo[pos]?.currentTemperature = nodeInfo[pos]?.currentTemperature?.plus(deltaTemperature) ?: 0.0
    }

    override fun modPressure(pos: DuctNodePos, deltaPressure: Double) {
        nodeInfo[pos]?.currentPressure = nodeInfo[pos]?.currentPressure?.plus(deltaPressure) ?: 0.0
    }

    override fun modGasVolume(pos: DuctNodePos, gasType: GasType, deltaVolume: Double) {
        nodeInfo[pos]?.currentGasMasses?.put(gasType, nodeInfo[pos]?.currentGasMasses?.get(gasType)?.plus(deltaVolume) ?: 0.0)
    }

    override fun tick(level: ServerLevel, subSteps: Int) {
        val invalidEdges = edges.filter { it.nodeA !in nodes || it.nodeB !in nodes }
        edges.removeAll(invalidEdges.toSet())

        for (step in 1..subSteps) {
            for (edge in edges) {
                val nodeA = nodeInfo[edge.nodeA]
                val nodeB = nodeInfo[edge.nodeB]

                val nodeDataA = nodes[edge.nodeA]!!
                val nodeDataB = nodes[edge.nodeB]!!

                if (unloadedNodes.contains(edge.nodeA) || unloadedNodes.contains(edge.nodeB)) {
                    continue
                }

                var madeNewA = false
                var madeNewB = false

                if (nodeA == null) {
                    nodeInfo[edge.nodeA] = DuctNodeInfo(nodes[edge.nodeA]!!.behavior,0.0, 0.0, EnumMap<GasType, Double>(GasType::class.java))
                    madeNewA = true
                }
                if (nodeB == null) {
                    nodeInfo[edge.nodeB] = DuctNodeInfo(nodes[edge.nodeB]!!.behavior,0.0, 0.0, EnumMap<GasType, Double>(GasType::class.java))
                    madeNewB = true
                }

                if (madeNewA || madeNewB) {
                    continue
                }

                var totalGasMassA = 0.0
                var totalGasMassB = 0.0

                nodeA!!.currentGasMasses.forEach { totalGasMassA += it.value }
                nodeB!!.currentGasMasses.forEach { totalGasMassB += it.value }

                if (totalGasMassA == 0.0 && totalGasMassB == 0.0) {
                    continue
                }

                val densityA = densityAverage(nodeA.currentGasMasses)
                val densityB = densityAverage(nodeB.currentGasMasses)

                val pressureA = calcPressure(totalGasMassA, nodeDataA.volume, nodeA.currentTemperature, densityA)
                val pressureB = calcPressure(totalGasMassB, nodeDataB.volume, nodeA.currentTemperature, densityB)

                val viscosityA = viscosityAverage(nodeA.currentGasMasses)
                val viscosityB = viscosityAverage(nodeB.currentGasMasses)

                nodeA.currentPressure = pressureA
                nodeB.currentPressure = pressureB

                val viscosity = (viscosityA + viscosityB) / 2.0

                var pumpPressureA = 0.0
                var pumpPressureB = 0.0

                if (nodeDataA.behavior == NodeBehaviorType.PUMP && (nodeDataA as PumpDuctNode).pumpTarget == edge) {
                    pumpPressureA = nodeDataA.pumpPressure
                }
                if (nodeDataB.behavior == NodeBehaviorType.PUMP && (nodeDataB as PumpDuctNode).pumpTarget == edge) {
                    pumpPressureB = nodeDataB.pumpPressure
                }

                var pumpPressure = pumpPressureA - pumpPressureB

                var aperture = 0.0
                if (edge is ApertureEdge) {
                    aperture = Math.min(edge.aperture, -edge.radius)
                }

                var flowRate = calculateFlow(pressureA, pressureB, edge.radius - aperture, viscosity, pumpPressure)

                if (edge is OneWayEdge) {
                    if (!edge.reversed && flowRate < 0.0) {
                        flowRate = 0.0
                    } else if (edge.reversed && flowRate > 0.0) {
                        flowRate = 0.0
                    }
                }

                if (flowRate.isInfinite() || flowRate.isNaN()) {
                    flowRate = 0.0
                }

                val flowRateA = flowRate
                val flowRateB = -flowRate

                for (gas in GasType.values()) {
                    if (flowRate == 0.0) {
                        continue
                    }
                    if (edge is FilteredEdge) {
                        if (edge.blacklist) {
                            if (edge.filter.contains(gas)) {
                                continue
                            }
                        } else {
                            if (!edge.filter.contains(gas)) {
                                continue
                            }
                        }
                    }

                    if (nodeA.currentGasMasses[gas] == null) {
                        nodeA.currentGasMasses[gas] = 0.0
                    }
                    if (nodeB.currentGasMasses[gas] == null) {
                        nodeB.currentGasMasses[gas] = 0.0
                    }

                    val volumeA = nodeA.currentGasMasses[gas]!!
                    val volumeB = nodeB.currentGasMasses[gas]!!

                    val deltaVolumeA = flowRateA / subSteps.toDouble()
                    val deltaVolumeB = flowRateB / subSteps.toDouble()

                    edge.currentFlowRate = flowRate

                    val deltaThermalEnergy = if (flowRate > 0) {
                        (totalGasMassA * specificHeatAverage(nodeA.currentGasMasses) * (nodeA.currentTemperature - nodeB.currentTemperature))
                    } else {
                        (totalGasMassB * specificHeatAverage(nodeB.currentGasMasses) * (nodeB.currentTemperature - nodeA.currentTemperature))
                    }

                    nodeA.currentGasMasses[gas] = min(volumeA + deltaVolumeA, 0.0)
                    nodeB.currentGasMasses[gas] = min(volumeB + deltaVolumeB, 0.0)

                    if (deltaThermalEnergy.isInfinite() || deltaThermalEnergy.isNaN()) {
                        continue
                    }

                    if (flowRate > 0) {
                        nodeA.currentTemperature -= deltaThermalEnergy / (totalGasMassA * specificHeatAverage(nodeA.currentGasMasses))
                        nodeB.currentTemperature += deltaThermalEnergy / (totalGasMassB * specificHeatAverage(nodeB.currentGasMasses))
                    } else {
                        nodeA.currentTemperature += deltaThermalEnergy / (totalGasMassA * specificHeatAverage(nodeA.currentGasMasses))
                        nodeB.currentTemperature -= deltaThermalEnergy / (totalGasMassB * specificHeatAverage(nodeB.currentGasMasses))
                    }
                }
            }
        }

        for (nodePos in nodeInfo.keys) {
            if (nodeInfo[nodePos] == null || nodes[nodePos] == null) {
                continue
            }

            val node = nodes[nodePos]!!
            val info = nodeInfo[nodePos]!!

            if (info.currentPressure > node.maxPressure) {
                level.explode(null, nodePos.x() + 0.5, nodePos.y() + 0.5, nodePos.z() + 0.5, 1f, Explosion.BlockInteraction.BREAK)
            }

//            if (info.currentPressure < node.minPressure) {
//                // todo wuh oh spaghettio prepare to implodeio
//            }
            //copilot wrote this so im immortalizing it

            //temperature control stuff
            if (info.currentTemperature < (node.maxTemperature/5) && info.previousTemperatureLevel != 0) {
                info.previousTemperatureLevel = 0
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.COOL)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            } else if (info.currentTemperature < ((2 * node.maxTemperature)/5) && info.previousTemperatureLevel != 1) {
                info.previousTemperatureLevel = 1
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.WARM)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            } else if (info.currentTemperature < ((3 * node.maxTemperature)/5) && info.previousTemperatureLevel != 2) {
                info.previousTemperatureLevel = 2
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.HOT)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            } else if (info.currentTemperature < ((4 * node.maxTemperature)/5) && info.previousTemperatureLevel != 3) {
                info.previousTemperatureLevel = 3
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.VERY_HOT)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            } else if (info.currentTemperature < node.maxTemperature && info.previousTemperatureLevel != 4) {
                info.previousTemperatureLevel = 4
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.SUPER_HOT)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            } else if (info.previousTemperatureLevel != 5) {
                info.previousTemperatureLevel = 5
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    val newState = level.getBlockState(BlockPos(nodePos.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, GasHeatLevel.MOLTEN)
                    level.setBlockAndUpdate(BlockPos(nodePos.toMinecraft()), newState)
                }
            }
        }
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double, density: Double): Double {
        val pressure: Double
        val molarMass = density * 22.4
        val moles = mass / molarMass
        pressure = (moles * idealGasConstant * temp) / volume
        return pressure
    }

    /**
     * Calculates the flow of gas based off pressure differentia, pipe radius, and viscosity using Poiseuille's Law.
     */
    private fun calculateFlow(pressureOne: Double, pressureTwo: Double, radius: Double, viscosity: Double, pumpPressure: Double = 0.0): Double {
        return ((pressureOne - pressureTwo + pumpPressure) * radius.pow(4.0)) / ((8.0/Math.PI) * viscosity * (10.0/16.0))
    }

    private fun densityAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] = massPerGas[it]!! + gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var density = 0.0

        for (gas in gasWeight.keys) {
            density += gasWeight[gas]!! * gas.density
        }

        return density
    }

    private fun viscosityAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] = massPerGas[it]!! + gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var viscosity = 0.0

        for (gas in gasWeight.keys) {
            viscosity += gasWeight[gas]!! * gas.viscosity
        }

        return viscosity
    }

    private fun specificHeatAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            if (gasMasses[it] != 0.0 ) {
                massPerGas[it] = massPerGas[it]!! + gasMasses[it]!!
            }

        }

        for (gas in massPerGas.keys) {
            gasWeight[gas] = massPerGas[gas]!! / totalMass
        }

        var specificHeat = 0.0

        for (gas in gasWeight.keys) {
            specificHeat += gasWeight[gas]!! * gas.specificHeatCapacity
        }

        return specificHeat
    }

    companion object {
        const val idealGasConstant = 8.314
    }
}