package org.valkyrienskies.clockwork.kelvin.impl

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.Explosion
import org.valkyrienskies.clockwork.ClockworkDamageSources
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.utilities.GasExplosionDamageCalculator
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.edges.ApertureEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.clockwork.kelvin.api.edges.OneWayEdge
import org.valkyrienskies.clockwork.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.clockwork.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.abs
import kotlin.math.max
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
        if (nodeInfo[node]?.currentPressure?.isNaN() == true) return 0.0
        return nodeInfo[node]?.currentPressure ?: 0.0
    }

    override fun getTemperatureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentTemperature ?: 0.0
    }

    override fun getGasVolumesAt(node: DuctNodePos): EnumMap<GasType, Double> {
        return nodeInfo[node]?.currentGasVolumes ?: EnumMap(GasType::class.java)
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
        nodeInfo[pos]?.currentGasVolumes?.put(gasType, nodeInfo[pos]?.currentGasVolumes?.get(gasType)?.plus(deltaVolume) ?: 0.0)
    }

    override fun modGasVolumeOfTemperature(pos: DuctNodePos, gasType: GasType, deltaVolume: Double, deltaTemperature: Double ) {
        var massInNode = 0.0
        nodeInfo[pos]?.currentGasVolumes?.forEach { massInNode += it.value*it.key.density } ?: return
        val specificHeatOfNode = specificHeatAverage(nodeInfo[pos]?.currentGasVolumes!!)
        val tempInNode = nodeInfo[pos]!!.currentTemperature
        val energyInNode = massInNode * specificHeatOfNode * tempInNode

        val deltaMass = deltaVolume * gasType.density


        val temp = (massInNode*specificHeatOfNode*tempInNode + deltaMass*deltaTemperature*gasType.specificHeatCapacity) / (massInNode*specificHeatOfNode + deltaMass*gasType.specificHeatCapacity)

        // = (deltaVolume*gasType.density*gasType.specificHeatCapacity*gasTemperature + massInNode*specificHeatOfNode*)/(deltaVolume*gasType.specificHeatCapacity + massInNode*specificHeatOfNode)

        nodeInfo[pos]!!.currentTemperature = temp

        modGasVolume(pos, gasType, deltaVolume)

    }

    override fun tick(level: ServerLevel, subSteps: Int) {
        if (disabled) return

        val invalidEdges = edges.keys.filter { it.first !in nodes || it.second !in nodes }
        for (edge in invalidEdges) {
            edges.remove(edge)
        }
        // TODO: Fix ConcurrentModificationException
        val edgesToProcess = HashMap(edges)
        for (step in 1..subSteps) {
            for (edgeKey in edgesToProcess.keys) {
                val edge = edgesToProcess[edgeKey]!!
                val nodeA = nodeInfo[edge.nodeA]
                val nodeB = nodeInfo[edge.nodeB]


                
                val nodeDataA = nodes[edge.nodeA] ?: continue
                val nodeDataB = nodes[edge.nodeB] ?: continue

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

                nodeA!!.currentGasVolumes.forEach { totalGasMassA += it.value }
                nodeB!!.currentGasVolumes.forEach { totalGasMassB += it.value }

                val heatCapacityA = specificHeatAverage(nodeA.currentGasVolumes)
                val heatCapacityB = specificHeatAverage(nodeB.currentGasVolumes)

                if (totalGasMassA == 0.0 && totalGasMassB == 0.0) {
                    continue
                }

                val densityA = densityAverage(nodeA.currentGasVolumes)
                val densityB = densityAverage(nodeB.currentGasVolumes)

                val tankMultA = if (nodeA.nodeType == NodeBehaviorType.TANK) (nodeDataA as TankDuctNode).size else 1.0
                val tankMultB = if (nodeB.nodeType == NodeBehaviorType.TANK) (nodeDataB as TankDuctNode).size else 1.0



                val pressureA = calcPressure(totalGasMassA, nodeDataA.volume, nodeA.currentTemperature, densityA)/tankMultA

                val pressureB = calcPressure(totalGasMassB, nodeDataB.volume, nodeB.currentTemperature, densityB)/tankMultB




                val viscosityA = viscosityAverage(nodeA.currentGasVolumes)
                val viscosityB = viscosityAverage(nodeB.currentGasVolumes)





                nodeA.currentPressure = pressureA

                nodeB.currentPressure = pressureB

                val viscosity = (viscosityA + viscosityB) / 2.0

                val aPump = nodeA.nodeType==NodeBehaviorType.PUMP
                val bPump = nodeB.nodeType==NodeBehaviorType.PUMP

                val aTarget = aPump && (nodeDataA as PumpDuctNode).pumpTarget == nodeDataB.pos
                val bTarget = bPump && (nodeDataB as PumpDuctNode).pumpTarget == nodeDataA.pos


                var pumpPressureA = 0.0
                var pumpPressureB = 0.0

                if (nodeDataA.behavior == NodeBehaviorType.PUMP) {
                    if (aTarget) pumpPressureA = (nodeDataA as PumpDuctNode).pumpPressure
                    else pumpPressureA = -(nodeDataA as PumpDuctNode).pumpPressure
                }
                if (nodeDataB.behavior == NodeBehaviorType.PUMP) {
                    if (bTarget) pumpPressureB = (nodeDataB as PumpDuctNode).pumpPressure
                    else pumpPressureB = -(nodeDataB as PumpDuctNode).pumpPressure
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

                val aFlowOut = flowRateA<0
                val bFlowOut = flowRateB<0

                var totalDeltaVolumeA = 0.0
                var totalDeltaVolumeB = 0.0

                val heatConductivityA = heatConductivityAverage(nodeA.currentGasVolumes, pressureA, nodeA.currentTemperature)
                val heatConductivityB = heatConductivityAverage(nodeB.currentGasVolumes, pressureB, nodeB.currentTemperature)

                val totalAvgHeatConductivity = (heatConductivityA + heatConductivityB) / 2.0

                val passiveHeatDelta = (totalAvgHeatConductivity * (Math.PI * edge.radius * 2.0) * ((nodeA.currentTemperature - nodeB.currentTemperature) / edge.length))
                val passiveHeatLimit = ((totalGasMassA * heatCapacityA * nodeA.currentTemperature) + (totalGasMassB * heatCapacityB * nodeB.currentTemperature))/2.0

                if (!passiveHeatDelta.isNaN() && passiveHeatLimit.isFinite()) {
                    if (totalGasMassA != 0.0 && totalGasMassB != 0.0) {
                        val deltaPassiveEnergy = Mth.clamp(passiveHeatDelta, -passiveHeatLimit, passiveHeatLimit) / subSteps.toDouble()
                        nodeA.currentTemperature -= deltaPassiveEnergy / (totalGasMassA * heatCapacityA)
                        nodeB.currentTemperature += deltaPassiveEnergy / (totalGasMassB * heatCapacityB)

                        nodeA.currentTemperature = max(nodeA.currentTemperature, 0.0)
                        nodeB.currentTemperature = max(nodeB.currentTemperature, 0.0)
                    }
                }

                nodeA.currentTemperature = max(nodeA.currentTemperature, 0.0)
                nodeB.currentTemperature = max(nodeB.currentTemperature, 0.0)

                val transferredGasses = EnumMap<GasType, Double>(GasType::class.java)

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

                    if (nodeA.currentGasVolumes[gas] == null) {
                        nodeA.currentGasVolumes[gas] = 0.0
                    }
                    if (nodeB.currentGasVolumes[gas] == null) {
                        nodeB.currentGasVolumes[gas] = 0.0
                    }




                    val volumeA = nodeA.currentGasVolumes[gas]!!
                    val volumeB = nodeB.currentGasVolumes[gas]!!


                    // This entire block is quite disgusting, but it serves a simple function.
                    // It lets pumps intake the entire volume of the node behind it, and outtake its own volume into the targetNode

                    val limit: Double
                    if (aTarget && aFlowOut || bPump && !bTarget && aFlowOut) limit = volumeA
                    else if (bTarget && bFlowOut || aPump && !aTarget && bFlowOut) limit = volumeB
                    else if (!aPump && !bPump) limit = abs(volumeA/tankMultA-volumeB/tankMultB)/2.0
                    else limit = 0.0




                    val deltaVolumeA = Mth.clamp(flowRateA, -limit, limit) / subSteps.toDouble()
                    val deltaVolumeB = Mth.clamp(flowRateB, -limit, limit) / subSteps.toDouble()



                    nodeA.currentGasVolumes[gas] = max(volumeA + deltaVolumeA, 0.0)
                    nodeB.currentGasVolumes[gas] = max(volumeB + deltaVolumeB, 0.0)

                    totalDeltaVolumeA += deltaVolumeA
                    totalDeltaVolumeB += deltaVolumeB
                    transferredGasses[gas] = deltaVolumeA
                }

                val totalTransferredMass = transferredGasses.values.sum()

                val flowHeatCapacity = specificHeatAverage(transferredGasses)

                var deltaThermalEnergy = if (abs(flowRate) > 0.0) {
                    (totalDeltaVolumeA * flowHeatCapacity * (nodeA.currentTemperature - nodeB.currentTemperature))
                } else {
                    0.0
                }

                val thermalLimit = if (flowRate > 0) {
                    totalGasMassA * heatCapacityA * nodeA.currentTemperature
                } else if (flowRate < 0) {
                    totalGasMassB * heatCapacityB * nodeB.currentTemperature
                } else {
                    0.0
                }
                deltaThermalEnergy = Mth.clamp(deltaThermalEnergy, -thermalLimit, thermalLimit)

                if (deltaThermalEnergy.isInfinite() || deltaThermalEnergy.isNaN()) continue

                val newTotalGasMassesA = nodeA.currentGasVolumes.values.sum()
                val newTotalGasMassesB = nodeB.currentGasVolumes.values.sum()
                val newHeatCapacityA = specificHeatAverage(nodeA.currentGasVolumes)
                val newHeatCapacityB = specificHeatAverage(nodeB.currentGasVolumes)

                if (newTotalGasMassesA != 0.0 && newTotalGasMassesB != 0.0) {
                    nodeA.currentTemperature -= deltaThermalEnergy / (newTotalGasMassesA * newHeatCapacityA)
                    nodeB.currentTemperature += deltaThermalEnergy / (newTotalGasMassesB * newHeatCapacityB)
                }

                nodeA.currentTemperature = max(nodeA.currentTemperature, 0.0)
                nodeB.currentTemperature = max(nodeB.currentTemperature, 0.0)

                edge.currentFlowRate = flowRate
            }
        }

        val nodesToSync = HashMap<DuctNodePos, GasHeatLevel>()
        val explnodes = HashSet<DuctNodePos>()

        for (nodePos in nodeInfo.keys) {
            if (nodeInfo[nodePos] == null || nodes[nodePos] == null) {
                continue
            }

            val node = nodes[nodePos]!!
            val info = nodeInfo[nodePos]!!

            if (info.currentPressure > node.maxPressure) {
                explnodes.add(nodePos)
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
            val state = level.getBlockState(BlockPos(node.toMinecraft()))
            if (state.block is DuctBlock) state.setValue(IHeatableBlock.GAS_HEAT_LEVEL, nodesToSync[node]!!)
            level.setBlockAndUpdate(BlockPos(node.toMinecraft()), state)
        }

        explnodes.forEach {
            level.explode(null, ClockworkDamageSources.GAS_EXPLOSION, GasExplosionDamageCalculator(),it.x() + 0.5, it.y() + 0.5, it.z() + 0.5, 1f, true, Explosion.BlockInteraction.BREAK)
        }
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double, density: Double): Double {
        if (volume == 0.0 || density == 0.0) return 0.0
        val adjustedTemp = max(temp,0.001)
        val pressure: Double
        val molarMass = density * 22.4
        val moles = mass / molarMass
        pressure = (moles * idealGasConstant * adjustedTemp) / volume
        if (pressure >= 1000000) println("pressure: $pressure, moles: $moles, adjustedTemp: $adjustedTemp, volume: $volume, mass: $mass, density: $density, molarMass: $molarMass, idealGasConstant: $idealGasConstant")
        
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

    private fun heatConductivityAverage(gasMasses: EnumMap<GasType, Double>, pressure: Double, temperature: Double): Double {
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

        var heatConductivity = 0.0

        for (gas in gasWeight.keys) {
            heatConductivity += gasWeight[gas]!! * ((gas.thermalConductivity * (temperature/300.0)) * (1.0 + (0.0075 * (pressure/101325.0))))
        }

        return heatConductivity
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
