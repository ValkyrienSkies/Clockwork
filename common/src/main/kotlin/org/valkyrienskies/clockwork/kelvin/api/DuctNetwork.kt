package org.valkyrienskies.clockwork.kelvin.api

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.clockwork.kelvin.impl.DuctNodeInfo
import org.valkyrienskies.core.impl.shadow.Du
import java.util.EnumMap

/**
 * The main class representing the Duct Network.
 */
interface DuctNetwork {

    var disabled: Boolean

    val nodes: HashMap<DuctNodePos, DuctNode>
    val edges: HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge>

    val nodeInfo: HashMap<DuctNodePos, DuctNodeInfo>

    val unloadedNodes: HashSet<DuctNodePos>

    fun markLoaded(pos: DuctNodePos)
    fun markUnloaded(pos: DuctNodePos)

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
    fun getGasMassAt(node: DuctNodePos): EnumMap<GasType, Double>

    fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge?
    fun getNodeAt(pos: DuctNodePos): DuctNode?

    fun addNode(pos: DuctNodePos, node: DuctNode)
    fun removeNode(pos: DuctNodePos)

    fun addEdge(posA: DuctNodePos, posB: DuctNodePos, edge: DuctEdge)
    fun removeEdge(posA: DuctNodePos, posB: DuctNodePos)

    fun modTemperature(pos: DuctNodePos, deltaTemperature: Double)
    fun modPressure(pos: DuctNodePos, deltaPressure: Double)
    fun modGasMass(pos: DuctNodePos, gasType: GasType, deltaMass: Double)

    fun modGasMassOfTemperature(pos: DuctNodePos, gasType: GasType, deltaMass: Double, gasTemperature: Double)
    fun modHeatEnergy(pos: DuctNodePos, deltaEnergy: Double)
    fun getHeatEnergy(pos: DuctNodePos): Double

    // the real meat
    fun tick(level: ServerLevel, subSteps: Int = 1)

    fun dump()

}