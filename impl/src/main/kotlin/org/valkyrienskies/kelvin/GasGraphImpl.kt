package org.valkyrienskies.kelvin

import java.util.EnumMap
import kotlin.math.abs
import kotlin.math.pow

class GasGraphImpl : GasGraph {
    private val nodes: MutableMap<GasNodeIdentifier, GasNode> = HashMap()

    private var queuedChanges: GasSimChangesFrame? = null

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

    private fun applyQueuedChanges() {
        val queuedChangesCopy = queuedChanges ?: return
        queuedChanges = null

        queuedChangesCopy.newNodes.forEach {
            addGasNode(it)
        }
        queuedChangesCopy.removedNodes.forEach {
            removeGasNode(it)
        }
        queuedChangesCopy.nodeChanges.forEach {
            nodes[it.identifier]?.applyChanges(it)
        }
        queuedChangesCopy.newConnections.forEach {
            connect(it)
        }
        queuedChangesCopy.removedConnections.forEach {
            disconnect(it)
        }
    }

    override fun tick(timeStep: Double, subSteps: Int): GasSimResultFrame {
        applyQueuedChanges()

        val frameChangeData: MutableMap<GasNodeIdentifier, GasNodeChangesData> = HashMap()

        val activeNodePressureData: MutableMap<GasNodeIdentifier, Double> = HashMap()

        //Calculate pressure
        nodes.keys.forEach {
            val nodeData = nodes[it]!!

            val gasMass: Double = nodeData.gasMasses.values.sum()

            activeNodePressureData[it] = calcPressure(gasMass, nodeData.volume, nodeData.temperature)
        }

        //Calculate flow

        val visitedConnections: HashSet<GasConnection> = HashSet()

        val collectedChangesData: HashMap<Int, GasNodeChangesData> = HashMap()
        var changesId = 0
        nodes.values.forEach {
            it.connections.keys.forEach {itConn ->
                if (!visitedConnections.contains(it.connections[itConn]!!)) {
                    visitedConnections.add(it.connections[itConn]!!)

                    val pressureOne = activeNodePressureData[it.identifier]!!

                    val pressureTwo = activeNodePressureData[itConn.identifier]!!

                    if (pressureOne != pressureTwo) {
                        val gasMasses = when {
                            pressureOne < pressureTwo -> it.gasMasses
                            else -> itConn.gasMasses
                        }

                        val avgViscosity = viscosityAverage(gasMasses)

                        val flow = poisuiellesLaw(pressureOne, pressureTwo, it.connections[itConn]!!.radius, avgViscosity, it.connections[itConn]!!.pumpPressureDrop ?: 0.0)

                        it.connections[itConn]!!.lastTickFlow = flow

                        val reverse = flow < 0.0
                        val flowAbs = abs(flow)
                        val returnVal = if (!reverse) {
                            propagateGas(it, itConn, flowAbs)
                        } else {
                            propagateGas(itConn, it, flowAbs)
                        }

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

        collectedChangesData.values.forEach {
            if (frameChangeData.containsKey(it.identifier)) {
                val existing = frameChangeData[it.identifier]!!
                existing.deltaGasMasses.keys.forEach { gasType ->
                    existing.deltaGasMasses[gasType] = existing.deltaGasMasses[gasType]!! + it.deltaGasMasses[gasType]!!
                }
                existing.deltaThermalEnergy += it.deltaThermalEnergy
                it.directionalDeltaMasses.keys.forEach { gasNodeIdentifier ->
                    if (existing.directionalDeltaMasses.containsKey(gasNodeIdentifier)) {
                        existing.directionalDeltaMasses[gasNodeIdentifier] = existing.directionalDeltaMasses[gasNodeIdentifier]!! + it.directionalDeltaMasses[gasNodeIdentifier]!!
                    } else {
                        existing.directionalDeltaMasses[gasNodeIdentifier] = it.directionalDeltaMasses[gasNodeIdentifier]!!
                    }
                }
            } else {
                frameChangeData[it.identifier] = it
            }
        }
        val results: HashMap<GasNodeIdentifier, GasNodeResultData> = HashMap()
        //apply changes
        frameChangeData.values.forEach {
            val result = nodes[it.identifier]?.applyChanges(it)
            if (result != null) results[it.identifier] = result
        }

        return GasSimResultFrame(results)
    }

    private fun propagateGas(from: GasNode, to: GasNode, flow: Double): Pair<GasNodeChangesData, GasNodeChangesData> {
        val timeAccFlowRate = flow / 1000.0

        val fromGasMasses = from.gasMasses
        val toGasMasses = to.gasMasses

        val fromGasMassesCopy = EnumMap<GasType, Double>(GasType::class.java)
        val toGasMassesCopy = EnumMap<GasType, Double>(GasType::class.java)

        fromGasMasses.keys.forEach {
            fromGasMassesCopy[it] = -flow
        }

        toGasMasses.keys.forEach {
            toGasMassesCopy[it] = flow
        }

        val fromAverageSpecificHeat = fromGasMasses.keys.sumOf { it.specificHeatCapacity } / fromGasMasses.values.size
        val thermalEnergyFrom = fromGasMasses.values.sum() * fromAverageSpecificHeat * (from.temperature - to.temperature)

        val deltaThermalEnergy = thermalEnergyFrom * timeAccFlowRate

        val fromChanges = GasNodeChangesData(from.identifier, fromGasMassesCopy, -deltaThermalEnergy, hashMapOf(to.identifier to flow))
        val toChanges = GasNodeChangesData(to.identifier, toGasMassesCopy, deltaThermalEnergy, hashMapOf(from.identifier to -flow))

        return Pair(fromChanges, toChanges)
    }

    /**
     * Calculates pressure using the ideal gas law.
     */
    private fun calcPressure(mass: Double, volume: Double, temp: Double): Double {
        var pressure = 0.0
        pressure = (mass * idealGasConstant * temp) / volume
        return pressure
    }

    /**
     * Yes I only named it this to be confusing. :clueless:
     *
     * Calculates the flow of gas based off a pressure differential as dictated by the titular law.
     */
    private fun poisuiellesLaw(pressureOne: Double, pressureTwo: Double, radius: Double, viscosity: Double, pumpPressure: Double = 0.0): Double {
        return ((pressureOne - pressureTwo + pumpPressure) * radius.pow(4.0)) / ((8.0/Math.PI) * viscosity * (10.0/16.0))
    }

    private fun viscosityAverage(gasMasses: EnumMap<GasType, Double>): Double {
        val totalMass = gasMasses.values.sum()

        if (totalMass == 0.0) {
            return 0.0
        }

        val massPerGas = EnumMap<GasType, Double>(GasType::class.java)

        val gasWeight = EnumMap<GasType, Double>(GasType::class.java)

        gasMasses.keys.forEach {
            massPerGas[it] = massPerGas[it]!! + gasMasses[it]!!
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

    override fun queueChanges(changesFrame: GasSimChangesFrame) {
        if (queuedChanges != null) {
            throw IllegalStateException("Cannot queue changes when we already have changes queued")
        }
        queuedChanges = changesFrame
    }
}
