package org.valkyrienskies.clockwork.content.kinetics.combobulator

internal data class EvaluationBatch<T>(
    val nodes: List<T>,
    val preTickReads: Set<T> = emptySet(),
)

internal object CombobulatorScheduler {
    /**
     * Condenses the directed dependency graph into Strongly Connected Components (Tarjan's algorithm)
     * in topological upstream-first order.
     *
     * - Acyclic nodes evaluate immediately after their upstream dependencies in the same tick.
     * - Cyclic SCCs containing sequential nodes break feedback at those sequential boundaries:
     *   the combinational interior evaluates first (reading pre-tick values from the sequential
     *   boundaries, and synchronously across any purely combinational sub-cycles), followed by
     *   the sequential boundaries updating synchronously from the settled interior.
     * - Purely combinational cyclic SCCs evaluate synchronously (intra-SCC edges read pre-tick
     *   values while incoming edges from upstream SCCs read current-tick values), keeping
     *   evaluation 100% independent of node or block-entity tick order.
     */
    fun <T> schedule(
        nodes: Collection<T>,
        dependencies: (T) -> Collection<T>,
        isSequential: (T) -> Boolean,
    ): List<EvaluationBatch<T>> {
        if (nodes.isEmpty()) return emptyList()
        val nodeSet = nodes.toSet()
        val adj = HashMap<T, List<T>>(nodeSet.size)
        for (node in nodeSet) {
            adj[node] = dependencies(node).filter { it in nodeSet }.distinct()
        }
        val batches = mutableListOf<EvaluationBatch<T>>()
        for (scc in tarjanSccs(nodes, adj)) {
            val cyclic = scc.size > 1 || scc[0] in adj[scc[0]].orEmpty()
            if (!cyclic) {
                batches += EvaluationBatch(scc)
                continue
            }
            val sequential = scc.filter(isSequential)
            if (sequential.isEmpty()) {
                batches += EvaluationBatch(scc, scc.toSet())
                continue
            }
            val sequentialSet = sequential.toSet()
            val combinational = scc.filterNot { it in sequentialSet }
            if (combinational.isNotEmpty()) {
                val combSet = combinational.toSet()
                val combAdj = HashMap<T, List<T>>(combSet.size)
                for (node in combSet) {
                    combAdj[node] = adj[node].orEmpty().filter { it in combSet }
                }
                for (subScc in tarjanSccs(combinational, combAdj)) {
                    val subCyclic = subScc.size > 1 || subScc[0] in combAdj[subScc[0]].orEmpty()
                    val preTick = if (subCyclic) sequentialSet + subScc else sequentialSet
                    batches += EvaluationBatch(subScc, preTick)
                }
            }
            batches += EvaluationBatch(sequential, sequentialSet)
        }
        return batches
    }

    private fun <T> tarjanSccs(
        nodes: Collection<T>,
        adj: Map<T, List<T>>,
    ): List<List<T>> {
        var nextIndex = 0
        val index = HashMap<T, Int>()
        val lowlink = HashMap<T, Int>()
        val onStack = HashSet<T>()
        val stack = ArrayDeque<T>()
        val result = mutableListOf<List<T>>()

        class Frame(val node: T, val neighbors: List<T>, var edgeIndex: Int = 0)
        val callStack = ArrayDeque<Frame>()

        for (start in nodes) {
            if (start in index) continue
            index[start] = nextIndex
            lowlink[start] = nextIndex
            nextIndex++
            stack.addLast(start)
            onStack.add(start)
            callStack.addLast(Frame(start, adj[start].orEmpty()))

            while (callStack.isNotEmpty()) {
                val frame = callStack.last()
                val v = frame.node
                if (frame.edgeIndex < frame.neighbors.size) {
                    val w = frame.neighbors[frame.edgeIndex++]
                    if (w !in index) {
                        index[w] = nextIndex
                        lowlink[w] = nextIndex
                        nextIndex++
                        stack.addLast(w)
                        onStack.add(w)
                        callStack.addLast(Frame(w, adj[w].orEmpty()))
                    } else if (w in onStack) {
                        lowlink[v] = minOf(lowlink.getValue(v), index.getValue(w))
                    }
                } else {
                    callStack.removeLast()
                    if (callStack.isNotEmpty()) {
                        val parent = callStack.last().node
                        lowlink[parent] = minOf(lowlink.getValue(parent), lowlink.getValue(v))
                    }
                    if (lowlink.getValue(v) == index.getValue(v)) {
                        val component = mutableListOf<T>()
                        while (true) {
                            val w = stack.removeLast()
                            onStack.remove(w)
                            component += w
                            if (w == v) break
                        }
                        result += component
                    }
                }
            }
        }
        return result
    }
}
