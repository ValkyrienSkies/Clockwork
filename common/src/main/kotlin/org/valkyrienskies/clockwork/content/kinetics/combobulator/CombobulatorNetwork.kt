package org.valkyrienskies.clockwork.content.kinetics.combobulator

import net.minecraft.server.level.ServerLevel
import java.util.IdentityHashMap

object CombobulatorNetwork {
    private class LevelState {
        val nodes = LinkedHashSet<CombobulatorBlockEntity>()
        var batches: List<EvaluationBatch<CombobulatorBlockEntity>> = emptyList()
        var wirelessEdges: Map<CombobulatorBlockEntity, List<CombobulatorBlockEntity>> = emptyMap()
        var dirty = false
    }

    private class ActiveEvaluation(val level: ServerLevel) {
        var preTickReads: Set<CombobulatorBlockEntity> = emptySet()
    }

    private val levels = IdentityHashMap<ServerLevel, LevelState>()
    private val active = ThreadLocal<ActiveEvaluation?>()

    fun register(level: ServerLevel, node: CombobulatorBlockEntity) {
        if (node.isRemoved || node.isVirtual) return
        val state = levels.getOrPut(level) { LevelState() }
        if (state.nodes.add(node)) state.dirty = true
    }

    fun unregister(level: ServerLevel, node: CombobulatorBlockEntity) {
        val state = levels[level] ?: return
        if (state.nodes.remove(node)) state.dirty = true
        if (state.nodes.isEmpty()) levels.remove(level)
    }

    fun markDirty(level: ServerLevel) {
        levels[level]?.dirty = true
    }

    fun clear(level: ServerLevel) {
        levels.remove(level)
    }

    fun clear() {
        levels.clear()
    }

    internal fun usesPreTickRead(level: ServerLevel, node: CombobulatorBlockEntity): Boolean {
        val current = active.get() ?: return false
        return current.level === level && node in current.preTickReads
    }

    fun tick(level: ServerLevel) {
        val state = levels[level] ?: return
        if (active.get() != null) return

        val removed = state.nodes.removeIf { it.isRemoved || it.isChunkUnloaded }
        if (removed) state.dirty = true
        if (state.nodes.isEmpty()) {
            levels.remove(level)
            return
        }

        if (!state.dirty && state.wirelessEdges.isNotEmpty()) {
            for ((node, previous) in state.wirelessEdges) {
                if (!level.isLoaded(node.blockPos)) continue
                if (node.wirelessUpstreamCombobulators(level) != previous) {
                    state.dirty = true
                    break
                }
            }
        }

        if (state.dirty) rebuild(level, state)

        val evaluated = mutableListOf<CombobulatorBlockEntity>()
        val evaluation = ActiveEvaluation(level)
        active.set(evaluation)
        try {
            for (batch in state.batches) {
                evaluation.preTickReads = batch.preTickReads
                val sampled = mutableListOf<Pair<CombobulatorBlockEntity, DoubleArray>>()
                for (node in batch.nodes) {
                    if (node.isRemoved || node.isChunkUnloaded || !level.shouldTickBlocksAt(node.blockPos)) continue
                    if (node.needsPreparation) node.ensurePrepared(level)
                    sampled += node to node.sampleInputs(level)
                }
                for ((node, inputs) in sampled) {
                    node.commitEvaluation(level, inputs)
                    evaluated += node
                }
            }
        } finally {
            active.remove()
        }

        for (node in evaluated) {
            if (!node.isRemoved && !node.isChunkUnloaded && level.isLoaded(node.blockPos)) {
                node.publishOutputs(level)
            }
        }
    }

    private fun rebuild(level: ServerLevel, state: LevelState) {
        val activeNodes = state.nodes.filter { !it.isRemoved && !it.isChunkUnloaded && level.isLoaded(it.blockPos) }
        for (node in activeNodes) node.ensurePrepared(level)

        val deps = HashMap<CombobulatorBlockEntity, List<CombobulatorBlockEntity>>(activeNodes.size)
        val wireless = HashMap<CombobulatorBlockEntity, List<CombobulatorBlockEntity>>()
        for (node in activeNodes) {
            deps[node] = node.upstreamCombobulators(level)
            if (node.hasWirelessInput) {
                wireless[node] = node.wirelessUpstreamCombobulators(level)
            }
        }
        state.batches = CombobulatorScheduler.schedule(
            nodes = activeNodes,
            dependencies = { deps[it].orEmpty() },
            isSequential = { it.isSequentialNode },
        )
        state.wirelessEdges = wireless
        state.dirty = false
    }
}
