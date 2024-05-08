package org.valkyrienskies.clockwork.kelvin.impl

import org.joml.Math.clamp
import org.valkyrienskies.clockwork.KelvinHandler
import org.valkyrienskies.clockwork.kelvin.api.GasConnectionCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasGraph
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesDataMutable
import org.valkyrienskies.clockwork.kelvin.api.GasNodeCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasNodeResultData
import org.valkyrienskies.clockwork.kelvin.api.GasSimChangesFrame
import org.valkyrienskies.clockwork.kelvin.api.GasSimResultFrame
import org.valkyrienskies.clockwork.kelvin.api.GasType
import java.util.EnumMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.pow

class GasGraphImpl : GasGraph {
    private val nodes: MutableMap<GasNodeIdentifier, GasNode> = HashMap()

    private val gameFramesQueue: ConcurrentLinkedQueue<GasSimChangesFrame> = ConcurrentLinkedQueue()

    private val idealGasConstant = 8.31446261815324

    /**
     * Return true if success
     */
    fun addGasNode(gasNodeCreateData: GasNodeCreateData): Boolean {
        val newNode = GasNode(
            gasNodeCreateData.identifier,
            EnumMap(gasNodeCreateData.gasMasses),
            gasNodeCreateData.volume,
            gasNodeCreateData.temperature,
            mutableMapOf(),
        )
        nodes[gasNodeCreateData.identifier] = newNode
        return true
    }

    /**
     * Return true if success
     */
    fun removeGasNode(identifier: GasNodeIdentifier): Boolean {

        val toDisconnect = mutableSetOf<GasNode>()

        toDisconnect.addAll(nodes[identifier]?.connections?.keys ?: emptySet())

        if (toDisconnect.isNotEmpty()) {
            toDisconnect.forEach {
                disconnect(Pair(identifier, it.identifier))
            }
        }

        return nodes.remove(identifier) != null
    }

    /**
     * Return true if success
     */
    fun connect(connectionCreateData: GasConnectionCreateData): Boolean {
        val nodeFrom = nodes[connectionCreateData.from] ?: return false
        val nodeTo = nodes[connectionCreateData.to] ?: return false

        val connection = GasConnection(
            nodeFrom.identifier,
            nodeTo.identifier,
            connectionCreateData.radius,
            connectionCreateData.lastTickFlow,
            connectionCreateData.pumpPressureDrop,
        )
        nodeFrom.connections[nodeTo] = connection
        nodeTo.connections[nodeFrom] = connection

        return true
    }

    /**
     * Return true if success
     */
    fun disconnect(connection: Pair<GasNodeIdentifier, GasNodeIdentifier>): Boolean {
        val first = nodes[connection.first] ?: return false
        val second = nodes[connection.second] ?: return false

        val firstRemoveResult = first.connections.remove(second) != null
        val secondRemoveResult = second.connections.remove(first) != null

        return firstRemoveResult && secondRemoveResult
    }

    private fun applyQueuedChanges(queuedChanges: GasSimChangesFrame) {
//        val queuedChangesCopy = queuedChanges ?: return
//        queuedChanges = null

        queuedChanges.newNodes.forEach {
            addGasNode(it)
        }
        queuedChanges.removedNodes.forEach {
            removeGasNode(it)
        }
        queuedChanges.nodeChanges.forEach {
            nodes[it.identifier]?.applyChanges(it)
        }
        queuedChanges.newConnections.forEach {
            connect(it)
        }
        queuedChanges.removedConnections.forEach {
            disconnect(it)
        }
    }

    override fun tick(timeStep: Double, subSteps: Int): GasSimResultFrame {
        val trueTimeStep = timeStep / subSteps.toDouble()

        while (gameFramesQueue.isNotEmpty()) {
            applyQueuedChanges(gameFramesQueue.remove())
        }

        for (subStep in 1..subSteps) {

            // Calculate pressure
            val activeNodePressureData: Map<GasNodeIdentifier, Double> = nodes.mapValues { (_, nodeData) ->
                val gasMass: Double = nodeData.gasMasses.values.sum()
                if (Epsilon.isEffectivelyZero(gasMass)) {
                    // No mass? No pressure.
                    return@mapValues 0.0
                }

                val weightedAverageDensity = nodeData.gasMasses.map { (gasType, mass) ->
                    gasType.density * mass
                }.sum() / gasMass

                return@mapValues calcPressure(gasMass, nodeData.volume, nodeData.temperature, weightedAverageDensity)
            }

            // Calculate flow
            val visitedConnections: HashSet<GasConnection> = HashSet()

            val collectedChangesData: HashMap<Int, GasNodeChangesDataMutable> = HashMap()
            var changesId = 0
            nodes.values.forEach {
                it.connections.keys.forEach inner@{ itConn ->
                    if (!visitedConnections.contains(it.connections[itConn]!!)) {
                        visitedConnections.add(it.connections[itConn]!!)

                        val pressureOne = activeNodePressureData[it.identifier]!!

                        val pressureTwo = activeNodePressureData[itConn.identifier]!!

                        if (pressureOne != pressureTwo) {
                            // TODO: Should we compute avgViscosity using the gas masses of both the nodes?
                            val gasMasses = when {
                                pressureOne >= pressureTwo -> it.gasMasses
                                else -> itConn.gasMasses
                            }

                            val avgViscosity = computeWeightedViscosity(gasMasses)

                            val flow = poisuiellesLaw(
                                pressureOne,
                                pressureTwo,
                                it.connections[itConn]!!.radius,
                                avgViscosity,
                                it.connections[itConn]!!.pumpPressureDrop ?: 0.0
                            )

                            it.connections[itConn]!!.lastTickFlow = flow

                            val reverse = flow < 0.0
                            val flowAbs = abs(flow)
                            val returnVal = if (!reverse) {
                                propagateGas(it, itConn, flowAbs, trueTimeStep)
                            } else {
                                propagateGas(itConn, it, flowAbs, trueTimeStep)
                            } ?: return@inner

                            val fromChanges = returnVal.first
                            val toChanges = returnVal.second

                            collectedChangesData[changesId] = fromChanges
                            changesId++
                            collectedChangesData[changesId] = toChanges
                            changesId++
                        }
                    }
                }
            }

            val frameChangeData: MutableMap<GasNodeIdentifier, GasNodeChangesDataMutable> = HashMap()
            collectedChangesData.values.forEach {
                if (frameChangeData.containsKey(it.identifier)) {
                    val existing = frameChangeData[it.identifier]!!
                    existing.deltaGasMasses.keys.forEach { gasType ->
                        existing.deltaGasMasses[gasType] =
                            (existing.deltaGasMasses[gasType] ?: 0.0) + it.deltaGasMasses[gasType]!!
                    }
                    existing.deltaThermalEnergy += it.deltaThermalEnergy
                    it.directionalDeltaMasses.keys.forEach { gasNodeIdentifier ->
                        if (existing.directionalDeltaMasses.containsKey(gasNodeIdentifier)) {
                            existing.directionalDeltaMasses[gasNodeIdentifier] =
                                (existing.directionalDeltaMasses[gasNodeIdentifier]
                                    ?: 0.0) + it.directionalDeltaMasses[gasNodeIdentifier]!!
                        } else {
                            existing.directionalDeltaMasses[gasNodeIdentifier] =
                                it.directionalDeltaMasses[gasNodeIdentifier] ?: 0.0
                        }
                    }
                } else {
                    frameChangeData[it.identifier] = it
                }
            }

            // Apply changes
            frameChangeData.forEach { (identifier, gasNodeChanges: GasNodeChangesData) ->
                nodes[identifier]?.applyChanges2(gasNodeChanges)
            }
        }



        val result = GasSimResultFrame(nodes.mapValues { (_, gasNode) ->
            GasNodeResultData(
                gasNode.gasMasses,
                gasNode.temperature,
            )
        })

        KelvinHandler.pushResultsFrame(result)

        return result
    }

    private fun propagateGas(from: GasNode, to: GasNode, flow: Double, timeStep: Double): Pair<GasNodeChangesDataMutable, GasNodeChangesDataMutable>? {
        val timeAccFlowRate = flow * timeStep

        val fromGasMasses = from.gasMasses

        val totalFromGasMass = fromGasMasses.values.sum()
        if (Epsilon.isEffectivelyZero(totalFromGasMass)) {
            return null
        }

        val fromGasFlows = EnumMap<GasType, Double>(GasType::class.java)
        val toGasFlows = EnumMap<GasType, Double>(GasType::class.java)

        fromGasMasses.forEach { (gasType, gasMass) ->
            val gasFlow = clamp(-gasMass, gasMass, timeAccFlowRate * gasMass / totalFromGasMass)
            fromGasFlows[gasType] = -gasFlow
            toGasFlows[gasType] = gasFlow
        }

        // TODO: Make this a weighted average
        val fromAverageSpecificHeat = if (fromGasMasses.isNotEmpty()) (fromGasMasses.keys.sumOf { it.specificHeatCapacity } / fromGasMasses.keys.size) else 0.0

        val thermalEnergyFrom = fromGasMasses.values.sum() * fromAverageSpecificHeat * (from.temperature - to.temperature)

        val deltaThermalEnergy = if (fromAverageSpecificHeat.isFinite()) {
            thermalEnergyFrom * timeAccFlowRate
        } else {
            0.0
        }

        if (!deltaThermalEnergy.isFinite()) {
            val fromChanges = GasNodeChangesDataMutable(from.identifier, fromGasFlows, -0.0, hashMapOf(to.identifier to toGasFlows.values.sum()))
            val toChanges = GasNodeChangesDataMutable(to.identifier, toGasFlows, 0.0, hashMapOf(from.identifier to fromGasFlows.values.sum()))
            return Pair(fromChanges, toChanges)
        }

        val fromChanges = GasNodeChangesDataMutable(from.identifier, fromGasFlows, -deltaThermalEnergy, hashMapOf(to.identifier to toGasFlows.values.sum()))
        val toChanges = GasNodeChangesDataMutable(to.identifier, toGasFlows, deltaThermalEnergy, hashMapOf(from.identifier to fromGasFlows.values.sum()))

        return Pair(fromChanges, toChanges)
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double, density: Double): Double {
        if (density == 0.0 || volume == 0.0) {
            return 0.0
        }
        val molarMass = density * 22.4
        val moles = mass / molarMass
        return (moles * idealGasConstant * temp) / volume
    }

    /**
     * Yes I only named it this to be confusing. :clueless:
     *
     * Calculates the flow of gas based off a pressure differential as dictated by the titular law.
     */
    private fun poisuiellesLaw(pressureOne: Double, pressureTwo: Double, radius: Double, viscosity: Double, pumpPressure: Double = 0.0): Double {
        return ((pressureOne - pressureTwo + pumpPressure) * radius.pow(4.0)) / ((8.0/Math.PI) * viscosity * (10.0/16.0))
    }

    private fun computeWeightedViscosity(gasMasses: EnumMap<GasType, Double>): Double {
        val totalGasMass = gasMasses.values.sum()
        if (totalGasMass < 1e-6) {
            return 0.0
        }
        return (gasMasses.map { (gasType, mass) -> gasType.viscosity * mass }.sum() / totalGasMass)
    }

    override fun queueChanges(changesFrame: GasSimChangesFrame) {
        if (gameFramesQueue.size > 100) {
            logger.warn("Changes queue is overloaded!")
            Thread.sleep(1000)
        }
        gameFramesQueue.add(changesFrame)
    }

    companion object {
        private val logger by logger("GasGraph")
    }
}
