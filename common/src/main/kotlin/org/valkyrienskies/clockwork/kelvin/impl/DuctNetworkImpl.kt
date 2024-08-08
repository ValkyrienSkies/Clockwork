package org.valkyrienskies.clockwork.kelvin.impl

import kotlinx.coroutines.flow.flow
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class DuctNetworkImpl(
    override var disabled: Boolean = true,
    override val nodes: HashMap<DuctNodePos, DuctNode> = hashMapOf(),
    override val edges: HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge> = hashMapOf(),
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
        return edges[Pair(from, to)] ?: edges[Pair(to, from)]
    }

    override fun getNodeAt(pos: DuctNodePos): DuctNode? {
        return nodes[pos]
    }

    override fun addNode(pos: DuctNodePos, node: DuctNode) {
        if (nodes.containsKey(pos) && nodes[pos]!!.behavior == node.behavior) {
            KELVINLOGGER.logger.info("Node already exists at $pos")
            return
        }
        nodes[pos] = node
        nodeInfo[pos] = DuctNodeInfo(node.behavior, 0.0, 0.0, EnumMap<GasType, Double>(GasType::class.java))
        KELVINLOGGER.logger.info("Added node at $pos")
    }

    override fun removeNode(pos: DuctNodePos) {
        val node = nodes.remove(pos)
        nodeInfo.remove(pos)

        if (unloadedNodes.contains(pos)) {
            unloadedNodes.remove(pos)
        }
        if (node != null) KELVINLOGGER.logger.info("Removed node at $pos")
    }

    override fun addEdge(posA: DuctNodePos, posB: DuctNodePos, edge: DuctEdge) {
        if (getEdgeBetween(posA, posB) != null && getEdgeBetween(posA, posB)!!.type == edge.type) {
            KELVINLOGGER.logger.info("Edge already exists between $posA and $posB")
            return
        }
        if (posA == posB) {
            return
        }
        edges[Pair(posA, posB)] = edge
        nodes[posA]?.nodeEdges?.add(edge)
        nodes[posB]?.nodeEdges?.add(edge)
        KELVINLOGGER.logger.info("Added edge between $posA and $posB")
    }

    override fun removeEdge(posA: DuctNodePos, posB: DuctNodePos) {
        val edge = edges.remove(Pair(posA, posB)) ?: edges.remove(Pair(posB, posA))
        if (edge != null) {
            nodes[posA]?.nodeEdges?.remove(edge)
            nodes[posB]?.nodeEdges?.remove(edge)
            KELVINLOGGER.logger.info("Removed edge between $posA and $posB")
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
        if (disabled) return

        val invalidEdges = edges.keys.filter { it.first !in nodes || it.second !in nodes }
        for (edge in invalidEdges) {
            edges.remove(edge)
        }
        val edgesToProcess = HashMap(edges)
        for (step in 1..subSteps) {
            for (edgeKey in edgesToProcess.keys) {
                val edge = edgesToProcess[edgeKey]!!
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

                val pressureB = calcPressure(totalGasMassB, nodeDataB.volume, nodeB.currentTemperature, densityB)


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

                val pumpPressure = pumpPressureA - pumpPressureB

                var aperture = 0.0
                if (edge is ApertureEdge) {
                    aperture = Math.max(edge.aperture, -edge.radius)
                }

                var flowRate = calculateFlow(pressureA, pressureB, edge.radius + aperture, viscosity, pumpPressure)


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

                val flowRateA = -flowRate
                val flowRateB = flowRate

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


                    val limit = abs(volumeA-volumeB)/2.0

                    val deltaVolumeA = Mth.clamp(flowRateA, -limit, limit) / subSteps.toDouble()
                    val deltaVolumeB = Mth.clamp(flowRateB, -limit, limit) / subSteps.toDouble()

                    nodeA.currentGasMasses[gas] = max(volumeA + deltaVolumeA, 0.0)
                    nodeB.currentGasMasses[gas] = max(volumeB + deltaVolumeB, 0.0)

                    totalGasMassA = nodeA.currentGasMasses.values.sum()
                    totalGasMassB = nodeB.currentGasMasses.values.sum()

                    val heatCapacityA = specificHeatAverage(nodeA.currentGasMasses)
                    val heatCapacityB = specificHeatAverage(nodeB.currentGasMasses)


                    edge.currentFlowRate = flowRate

                    var deltaThermalEnergy = if (flowRate > 0) {
                        (totalGasMassA * heatCapacityA * (nodeA.currentTemperature - nodeB.currentTemperature))
                    } else if (flowRate < 0) {
                        (totalGasMassB * heatCapacityB * (nodeB.currentTemperature - nodeA.currentTemperature))
                    } else {
                        0.0
                    }

                    val thermalLimit = abs((totalGasMassA*heatCapacityA*nodeA.currentTemperature)-(totalGasMassB*heatCapacityB*nodeB.currentTemperature))/2.0
                    deltaThermalEnergy = Mth.clamp(deltaThermalEnergy, -thermalLimit, thermalLimit)/subSteps.toDouble()


                    if (deltaThermalEnergy.isInfinite() || deltaThermalEnergy.isNaN()) return


                    if (flowRate > 0) {
                        if (totalGasMassA > 0) nodeA.currentTemperature -= deltaThermalEnergy / (totalGasMassA * heatCapacityA)
                        if (totalGasMassB > 0) nodeB.currentTemperature += deltaThermalEnergy / (totalGasMassB * heatCapacityB)
                    } else {
                        if (totalGasMassA > 0) nodeA.currentTemperature += deltaThermalEnergy / (totalGasMassA * heatCapacityA)
                        if (totalGasMassB > 0) nodeB.currentTemperature -= deltaThermalEnergy / (totalGasMassB * heatCapacityB)
                    }

                }
            }
        }

        val nodesToSync = HashMap<DuctNodePos, GasHeatLevel>()

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
                    nodesToSync[nodePos] = GasHeatLevel.COOL
                }
            } else if (info.currentTemperature >= (node.maxTemperature/5) && info.currentTemperature < ((2 * node.maxTemperature)/5) && info.previousTemperatureLevel != 1) {
                info.previousTemperatureLevel = 1
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    nodesToSync[nodePos] = GasHeatLevel.WARM
                }
            } else if (info.currentTemperature >= ((2 * node.maxTemperature)/5) && info.currentTemperature < ((3 * node.maxTemperature)/5) && info.previousTemperatureLevel != 2) {
                info.previousTemperatureLevel = 2
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    nodesToSync[nodePos] = GasHeatLevel.HOT
                }
            } else if (info.currentTemperature >= ((3 * node.maxTemperature)/5) && info.currentTemperature < ((4 * node.maxTemperature)/5) && info.previousTemperatureLevel != 3) {
                info.previousTemperatureLevel = 3
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    nodesToSync[nodePos] = GasHeatLevel.VERY_HOT
                }
            } else if (info.currentTemperature >= ((4 * node.maxTemperature)/5) && info.currentTemperature < node.maxTemperature && info.previousTemperatureLevel != 4) {
                info.previousTemperatureLevel = 4
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    nodesToSync[nodePos] = GasHeatLevel.SUPER_HOT
                }
            } else if (info.currentTemperature >= node.maxTemperature && info.previousTemperatureLevel != 5) {
                info.previousTemperatureLevel = 5
                if (level.getBlockState(BlockPos(nodePos.toMinecraft())).block is IHeatableBlock) {
                    nodesToSync[nodePos] = GasHeatLevel.MOLTEN
                }
            }
        }

        for (node in nodesToSync.keys) {
            level.setBlockAndUpdate(BlockPos(node.toMinecraft()), level.getBlockState(BlockPos(node.toMinecraft())).setValue(IHeatableBlock.GAS_HEAT_LEVEL, nodesToSync[node]!!))
        }
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double, density: Double): Double {
        if (volume == 0.0 || density == 0.0) return 0.0
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


                massPerGas[it] =  gasMasses[it]!!

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
                massPerGas[it] = gasMasses[it]!!
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
                massPerGas[it] =  gasMasses[it]!!
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

    override fun dump() {
        KELVINLOGGER.logger.info("Disabling Kelvin...")

        disabled = true

        KELVINLOGGER.logger.info("Dumping Kelvin information...")

        edges.clear()
        nodes.clear()
        nodeInfo.clear()

        unloadedNodes.clear()

        KELVINLOGGER.logger.info("Dumped Kelvin information. Now get out!")
    }

    companion object {
        const val idealGasConstant = 8.314

        val KELVINLOGGER = logger("fart factory")
    }
}
