package org.valkyrienskies.clockwork

import dev.architectury.event.events.common.TickEvent
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.logistics.heat.IHeatable
import org.valkyrienskies.clockwork.kelvin.api.*
import org.valkyrienskies.mod.common.util.toBlockPos
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

object KelvinHandler {
    init {
        TickEvent.SERVER_LEVEL_POST.register {
            tick(it)
        }
    }

    // to sim
    private val newNodes: MutableList<GasNodeCreateData> = mutableListOf()
    private val removedNodes: MutableList<GasNodeIdentifier> = mutableListOf()
    private val nodeChanges: MutableList<GasNodeChangesData> = mutableListOf()
    private val newConnections: MutableList<GasConnectionCreateData> = mutableListOf()
    private val removedConnections: MutableList<Pair<GasNodeIdentifier, GasNodeIdentifier>> = mutableListOf()

    // from sim
    private val gasSimResultQueue: ConcurrentLinkedQueue<GasSimResultFrame> = ConcurrentLinkedQueue()

    val nodes: HashSet<GasNodeIdentifier> = hashSetOf()

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
            removedConnections
        )
        pushChangesFrame(changesFrame)

        newNodes.clear()
        removedNodes.clear()
        nodeChanges.clear()
        newConnections.clear()
        removedConnections.clear()
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