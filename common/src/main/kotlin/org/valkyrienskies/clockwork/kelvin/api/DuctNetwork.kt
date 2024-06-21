package org.valkyrienskies.clockwork.kelvin.api

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.kelvin.impl.DuctNodeInfo
import java.util.EnumMap

/**
 * The main class representing the Duct Network.
 */
interface DuctNetwork {

    val nodes: HashMap<DuctNodePos, DuctNode>
    val edges: HashSet<DuctEdge>

    val nodeInfo: HashMap<DuctNodePos, DuctNodeInfo>

    // interfacing with the duct network
    /**
     * Returns the flow between two nodes from the previous tick.
     */
    fun getFlowBetween(from: DuctNodePos, to: DuctNodePos): Double

    /**
     * Returns the pressure at a node from the previous tick.
     */
    fun getPressureAt(node: DuctNodePos): Double

    /**
     * Returns the temperature at a node from the previous tick.
     */
    fun getTemperatureAt(node: DuctNodePos): Double

    /**
     * Returns the gas volumes at a node from the previous tick.
     */
    fun getGasVolumesAt(node: DuctNodePos): EnumMap<GasType, Double>

    fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge?
    fun getNodeAt(pos: DuctNodePos): DuctNode?

    fun addNode(pos: DuctNodePos, node: DuctNode)
    fun removeNode(pos: DuctNodePos)

    fun addEdge(posA: DuctNodePos, posB: DuctNodePos, edge: DuctEdge)
    fun removeEdge(posA: DuctNodePos, posB: DuctNodePos)

    fun modTemperature(pos: DuctNodePos, deltaTemperature: Double)
    fun modPressure(pos: DuctNodePos, deltaPressure: Double)
    fun modGasVolume(pos: DuctNodePos, gasType: GasType, deltaVolume: Double)

    // the real meat
    fun tick(level: ServerLevel, subSteps: Int = 1)

}