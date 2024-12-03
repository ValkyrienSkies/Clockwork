package org.valkyrienskies.clockwork.kelvin.impl.client

import net.minecraft.client.multiplayer.ClientLevel
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork.Companion.KELVINLOGGER
import org.valkyrienskies.clockwork.kelvin.impl.DuctNodeInfo
import org.valkyrienskies.clockwork.util.AerodynamicUtils.specificHeatAverage
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class DuctNetworkClient: DuctNetwork<ClientLevel> {

    override var disabled = true

    //Always empty on Client.
    override val nodes = HashMap<DuctNodePos, DuctNode>()

    override val nodeInfo = HashMap<DuctNodePos, DuctNodeInfo>()

    //Always empty on Client.
    override val edges = HashMap<Pair<DuctNodePos, DuctNodePos>, DuctEdge>()
    override val unloadedNodes = HashSet<DuctNodePos>()

    private var ticksSinceLastSync = 0

    fun queryTicksSinceLastSync(): Int {
        return ticksSinceLastSync
    }

    override fun tick(level: ClientLevel, subSteps: Int) {
        if (disabled) return

        ticksSinceLastSync++
    }

    fun sync(info: ClientKelvinInfo) {
        nodeInfo.clear()
        nodeInfo.putAll(info.nodes)
        ticksSinceLastSync = 0
    }

    override fun dump() {
        nodeInfo.clear()
    }

    override fun markLoaded(pos: DuctNodePos) {
        KELVINLOGGER.logger.warn("Client does not have access to this information. [markLoaded]")
    }

    override fun markUnloaded(pos: DuctNodePos) {
        KELVINLOGGER.logger.warn("Client does not have access to this information. [markUnloaded]")
    }

    override fun getFlowBetween(from: DuctNodePos, to: DuctNodePos): Double {
        KELVINLOGGER.logger.warn("Client does not have access to this information. [getFlowBetween]")
        return -1.0
    }

    override fun getPressureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentPressure ?: -1.0
    }

    override fun getTemperatureAt(node: DuctNodePos): Double {
        return nodeInfo[node]?.currentTemperature ?: -1.0
    }

    override fun getGasMassAt(node: DuctNodePos): EnumMap<GasType, Double> {
        return nodeInfo[node]?.currentGasMasses ?: EnumMap(GasType::class.java)
    }

    override fun getEdgeBetween(from: DuctNodePos, to: DuctNodePos): DuctEdge? {
        KELVINLOGGER.logger.warn("Client does not have access to this information. [getEdgeBetween]")
        return null
    }

    override fun getNodeAt(pos: DuctNodePos): DuctNode? {
        KELVINLOGGER.logger.warn("Client does not have access to this information. [getNodeAt]")
        return null
    }

    override fun addNode(pos: DuctNodePos, node: DuctNode) {
        KELVINLOGGER.logger.warn("Client can't add nodes.")
    }

    override fun removeNode(pos: DuctNodePos) {
        nodeInfo.remove(pos)
    }



    override fun getHeatEnergy(pos: DuctNodePos): Double {
        return getTemperatureAt(pos) * specificHeatAverage(getGasMassAt(pos)) * getGasMassAt(pos).values.sum()
    }
}