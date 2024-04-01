package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.TickEvent
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.clockwork.kelvin.api.GasConnectionCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeChangesData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeCreateData
import org.valkyrienskies.clockwork.kelvin.api.GasNodeId
import org.valkyrienskies.clockwork.kelvin.api.GasNodeIdentifier
import org.valkyrienskies.clockwork.kelvin.api.GasSimChangesFrame
import org.valkyrienskies.clockwork.kelvin.api.GasSimResultFrame
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.mod.common.util.toBlockPos
import java.util.EnumMap
import java.util.concurrent.ConcurrentLinkedQueue

object KelvinHandler {
    init {
        TickEvent.SERVER_LEVEL_POST.register {
            tick(it)
        }
    }

    // to sim
    private var newNodes: MutableList<GasNodeCreateData> = mutableListOf()
    private var removedNodes: MutableList<GasNodeIdentifier> = mutableListOf()
    private var nodeChanges: MutableList<GasNodeChangesData> = mutableListOf()
    private var newConnections: MutableList<GasConnectionCreateData> = mutableListOf()
    private var removedConnections: MutableList<Pair<GasNodeIdentifier, GasNodeIdentifier>> = mutableListOf()

    // from sim
    private val gasSimResultQueue: ConcurrentLinkedQueue<GasSimResultFrame> = ConcurrentLinkedQueue()

    private val nodes: HashSet<GasNodeIdentifier> = hashSetOf()

    fun tick(serverLevel: ServerLevel) {
        while (gasSimResultQueue.isNotEmpty()) {
            applyResults(gasSimResultQueue.remove(), serverLevel)
        }

        nodes.addAll(newNodes.map { it.identifier })
        nodes.removeAll(removedNodes.toSet())

        val changesFrame = GasSimChangesFrame(
            newNodes,
            removedNodes,
            nodeChanges,
            newConnections,
            removedConnections,
        )

        // Update the fields to prevent concurrent modification
        newNodes = mutableListOf()
        removedNodes = mutableListOf()
        nodeChanges = mutableListOf()
        newConnections = mutableListOf()
        removedConnections = mutableListOf()

        pushChangesFrame(changesFrame)
    }

    /**
     * Returns the first node with a matching position. Breaks for multiple nodes at the same position.
     */
    fun getNodeFromPos(pos: Vector3ic): GasNodeIdentifier? {
        nodes.forEach { if (it.pos == pos) return it }
        return null
    }

    private fun applyResults(frame: GasSimResultFrame, slevel: ServerLevel) {
        for (id in frame.nodesData.keys) {
            val update = frame.nodesData[id]!!
            val blockPos = id.pos.toBlockPos()
            val blockEntity = slevel.getBlockEntity(blockPos)
            if (blockEntity is IHeatable) {
                blockEntity.applyUpdate(update)
            }
        }
    }

    private fun pushChangesFrame(frame: GasSimChangesFrame) {
        ClockworkMod.getKelvinBackgroundTask().gasGraph.queueChanges(frame)
    }

    fun pushResultsFrame(frame: GasSimResultFrame) {
        gasSimResultQueue.add(frame)
    }

    // game funcs

    fun addNode(createData: GasNodeCreateData) {
        newNodes.add(createData)
    }

    fun delNode(nodeIdentifier: GasNodeIdentifier) {
        removedNodes.add(nodeIdentifier)
    }

    fun editNode(change: GasNodeChangesData) {
        nodeChanges.add(change)
    }

    fun connectNodes(connection: GasConnectionCreateData) {
        newConnections.add(connection)
    }

    fun disconnectNodes(id1: GasNodeIdentifier, id2: GasNodeIdentifier) {
        removedConnections.add(id1 to id2)
    }

    /**
     * For pipes.
     */
    fun defaultGasNodeCreateData(pos: Vector3ic, id: GasNodeId = 0, initTemp: Double = 0.0): GasNodeCreateData {
        return GasNodeCreateData(
            GasNodeIdentifier(pos, id),
            EnumMap(GasType::class.java),
            0.375,
            initTemp
        )
    }

    // empty initialization function
    fun start() {}
}