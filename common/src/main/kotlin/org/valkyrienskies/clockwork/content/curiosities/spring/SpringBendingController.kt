package org.valkyrienskies.clockwork.content.curiosities.spring

import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import java.util.concurrent.ConcurrentHashMap

internal object SpringBendingController {
    private data class End(val ship: Long, val position: Vector3d, val normal: Vector3d, val side: Vector3d)
    private data class Spring(val a: End, val b: End, val joint: Int, val alignment: Int, val mass: Double, val settings: SpringSettings)
    private data class Snapshot(val springs: List<Spring>, val expires: Long)
    private val snapshots = ConcurrentHashMap<String, Snapshot>()

    fun publish(dimension: String, links: Collection<WandLinks.Link>) {
        fun end(anchor: WandAnchor): End {
            val p = anchor.local()
            val n = Vector3d(anchor.face.stepX.toDouble(), anchor.face.stepY.toDouble(), anchor.face.stepZ.toDouble())
            return End(anchor.shipId, Vector3d(p.x, p.y, p.z), n,
                if (kotlin.math.abs(n.x) < 0.5) Vector3d(1.0, 0.0, 0.0) else Vector3d(0.0, 1.0, 0.0))
        }
        val springs = links.filter { it.spring && it.physical && !it.suspended && !it.pending &&
            it.jointId != null && it.alignmentId != null }.map {
            Spring(end(it.a), end(it.b), it.jointId!!, it.alignmentId!!, it.mass, it.settings)
        }
        if (springs.isEmpty()) snapshots.remove(dimension)
        else snapshots[dimension] = Snapshot(springs, System.nanoTime() + 1_000_000_000L)
    }

    fun tick(level: PhysLevel) {
        val snapshot = snapshots[level.dimension] ?: return
        if (System.nanoTime() > snapshot.expires) { snapshots.remove(level.dimension, snapshot); return }
        val joints = level as? VsiPhysLevel ?: return
        for (spring in snapshot.springs) {
            if (joints.getJointById(spring.joint) == null || joints.getJointById(spring.alignment) == null) continue
            val a = if (spring.a.ship < 0) null else (level.getShipById(spring.a.ship) ?: continue)
            val b = if (spring.b.ship < 0) null else (level.getShipById(spring.b.ship) ?: continue)
            fun position(end: End, ship: PhysShip?) = ship?.transform?.shipToWorld?.transformPosition(end.position, Vector3d()) ?: Vector3d(end.position)
            fun vector(v: Vector3d, ship: PhysShip?) = ship?.transform?.shipToWorldRotation?.transform(v, Vector3d()) ?: Vector3d(v)
            fun velocity(ship: PhysShip?, point: Vector3d): Vector3d {
                if (ship == null) return Vector3d()
                val center = ship.transform.shipToWorld.transformPosition(ship.centerOfMass, Vector3d())
                return ship.angularVelocity.cross(point.sub(center, Vector3d()), Vector3d()).add(ship.velocity)
            }
            val pointA = position(spring.a, a)
            val pointB = position(spring.b, b)
            val chord = pointB.sub(pointA, Vector3d())
            if (chord.lengthSquared() < 1e-6) continue
            val direction = Vector3d(chord).normalize()
            val chordAngularVelocity = chord.cross(velocity(b, pointB).sub(velocity(a, pointA)), Vector3d()).div(chord.lengthSquared())
            val torqueA = SpringBending.torque(vector(spring.a.normal, a), direction, vector(spring.a.side, a),
                Vector3d(a?.angularVelocity ?: Vector3d()).sub(chordAngularVelocity), spring.mass, spring.settings)
            val torqueB = SpringBending.torque(vector(spring.b.normal, b), Vector3d(direction).negate(), vector(spring.b.side, b),
                Vector3d(b?.angularVelocity ?: Vector3d()).sub(chordAngularVelocity), spring.mass, spring.settings)
            val force = SpringBending.reactionForce(torqueA, torqueB, chord)
            val limit = spring.mass * 120.0
            if (force.lengthSquared() > limit * limit) {
                val scale = limit / force.length()
                force.mul(scale); torqueA.mul(scale); torqueB.mul(scale)
            }
            a?.applyWorldTorque(torqueA)
            b?.applyWorldTorque(torqueB)
            a?.applyWorldForceToModelPos(force, spring.a.position)
            b?.applyWorldForceToModelPos(Vector3d(force).negate(), spring.b.position)
        }
    }
}
