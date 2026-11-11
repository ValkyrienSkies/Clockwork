package org.valkyrienskies.clockwork.util

import org.valkyrienskies.core.internal.joints.*

internal fun VSJoint.carriesLinearLoad(): Boolean = when (this) {
    is VSFixedJoint, is VSRevoluteJoint, is VSSphericalJoint -> true
    is VSD6Joint -> listOf(VSD6Joint.D6Axis.X, VSD6Joint.D6Axis.Y, VSD6Joint.D6Axis.Z).all {
        (motions?.get(it) ?: VSD6Joint.D6Motion.LOCKED) == VSD6Joint.D6Motion.LOCKED
    }
    else -> false
}

internal class ConnectedShipMass(joints: Collection<VSJoint>,
                                 private val groundBody: Long? = null,
                                 elastic: (VSJoint) -> Boolean = { it is VSSpringJoint },
                                 private val body: (Long) -> Load?) {
    data class Load(val mass: Double, val anchored: Boolean = false)
    private class Component(val load: Load, val available: Boolean = true)

    private val edges = HashMap<Long, MutableSet<Long?>>()
    private val components = HashMap<Long?, Component>()
    private val elasticJoints = joints.filter { !it.carriesLinearLoad() && elastic(it) }
    private val results = HashMap<Pair<Component, Component>, Pair<Load, Load>>()
    private val connections by lazy {
        val graph = HashMap<Component, MutableSet<Component>>()
        for (joint in elasticJoints) {
            val a = component(joint.shipId0)
            val b = component(joint.shipId1)
            if (a === b || !a.available || !b.available) continue
            graph.getOrPut(a) { HashSet() }.add(b)
            graph.getOrPut(b) { HashSet() }.add(a)
        }
        graph
    }

    init {
        for (joint in joints) {
            if (!joint.carriesLinearLoad()) continue
            val a = normalize(joint.shipId0)
            val b = normalize(joint.shipId1)
            a?.let { edges.getOrPut(it) { HashSet() }.add(b) }
            b?.let { edges.getOrPut(it) { HashSet() }.add(a) }
        }
    }

    fun endpoints(a: Long?, b: Long?): Pair<Load, Load> {
        val left = component(a)
        val right = component(b)
        if (left === right || (left.load.anchored && right.load.anchored)) return direct(a) to direct(b)
        results[left to right]?.let { return it }
        val leftBodies = connected(left, left, right)
        val rightBodies = connected(right, left, right)
        val loadA = Load(leftBodies.sumOf { it.load.mass }, left.load.anchored)
        val loadB = Load(rightBodies.sumOf { it.load.mass }, right.load.anchored)
        val result = if (leftBodies.any(rightBodies::contains) || (loadA.anchored && loadB.anchored))
            left.load to right.load
        else loadA to loadB
        results[left to right] = result
        results[right to left] = result.second to result.first
        return result
    }

    fun sameRigidComponent(a: Long?, b: Long?): Boolean = component(a) === component(b)

    fun total(id: Long): Load {
        val root = component(id)
        return Load(connected(root, root, root).sumOf { it.load.mass }, root.load.anchored)
    }

    fun rigidMass(id: Long): Double = component(id).load.mass

    private fun normalize(id: Long?): Long? = id.takeUnless { it == groundBody }
    private fun direct(id: Long?): Load = normalize(id)?.let { body(it) ?: Load(0.0) } ?: Load(0.0, true)

    private fun connected(start: Component, a: Component, b: Component): Set<Component> {
        val visited = HashSet<Component>()
        val queue = ArrayDeque<Component>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current) || current.load.anchored) continue
            for (next in connections[current].orEmpty()) {
                if ((current === a && next === b) || (current === b && next === a)) continue
                if (next !in visited) queue.add(next)
            }
        }
        return visited
    }

    private fun component(startId: Long?): Component {
        val start = normalize(startId)
        components[start]?.let { return it }
        if (start == null) return Component(Load(0.0, true)).also { components[start] = it }
        val first = body(start) ?: return Component(Load(0.0), false).also { components[start] = it }
        if (first.anchored) return Component(Load(0.0, true)).also { components[start] = it }
        val visited = HashSet<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(start)
        var mass = 0.0
        var anchored = false
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (id in visited) continue
            val ship = body(id) ?: continue
            if (ship.anchored) {
                anchored = true
                continue
            }
            visited.add(id)
            if (ship.mass.isFinite() && ship.mass > 0.0) mass += ship.mass
            for (other in edges[id].orEmpty()) {
                if (other == null) anchored = true else if (other !in visited) queue.add(other)
            }
        }
        return Component(Load(mass, anchored)).also { component ->
            for (id in visited) components[id] = component
        }
    }
}
