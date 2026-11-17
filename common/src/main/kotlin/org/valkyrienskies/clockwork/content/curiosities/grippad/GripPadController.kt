package org.valkyrienskies.clockwork.content.curiosities.grippad

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix3d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.util.ConnectedShipMass
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.api.physics.ContactDiagnosticsFrame
import org.valkyrienskies.core.api.physics.ContactDiagnosticsOptions
import org.valkyrienskies.core.api.physics.ContactDiagnosticsSubscription
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

internal object GripPadController {
    data class Key(val dimension: String, val pos: BlockPos)
    data class Pad(val ship: Long?, val body: Long?, val face: Direction, val powered: Boolean, val expires: Long)
    data class Feedback(val contact: Boolean, val gripping: Boolean, val impact: Double,
                        val normalLoad: Double?, val slipSpeed: Double, val expires: Long, val support: Support? = null)
    data class Support(val source: Long?, val target: Long, val upward: Double, val points: List<Vector3dc>)
    private data class Grip(val source: Long?, val target: Long, val point: Vector3d, val face: Direction)
    private data class Contact(val key: Key, val pad: Pad, val source: PhysVsBody?, val target: PhysVsBody,
                               val point: Vector3d, val normal: Vector3d, val facing: Vector3d,
                               val velocity: Vector3d, val impact: Double)
    private class MeasuredLoad {
        var force = 0.0
        var latest = Double.NEGATIVE_INFINITY
        val points = ArrayList<Vector3dc>()

        fun add(load: Double, time: Double, point: Vector3dc) {
            force += load
            if (time < latest) return
            if (time > latest) {
                latest = time
                points.clear()
            }
            if (points.size < 32) points.add(point)
        }
    }
    private data class Load(val body: Long?, val target: Long, val face: Direction, val force: Double,
                            val pointsInTarget: List<Vector3dc>)
    private val pads = ConcurrentHashMap<Key, Pad>()
    private val feedback = ConcurrentHashMap<Key, Feedback>()
    private val grips = HashMap<Key, Grip>()
    private val loads = HashMap<Key, Load>()
    private val diagnostics = HashMap<PhysLevel, ContactDiagnosticsSubscription?>()

    fun publish(key: Key, ship: Long?, body: Long?, face: Direction, powered: Boolean) {
        pads[key] = Pad(ship, body, face, powered, System.nanoTime() + 1_000_000_000L)
    }

    fun remove(key: Key) { pads.remove(key); feedback.remove(key) }
    fun feedback(key: Key) = feedback[key]?.takeIf { it.expires > System.nanoTime() }

    fun clear() {
        pads.clear()
        feedback.clear()
        grips.clear()
        loads.clear()
        diagnostics.clear()
    }

    fun tick(level: PhysLevel, delta: Double) {
        val now = System.nanoTime()
        val contacts = ArrayList<Contact>()
        var hasPads = false
        for ((key, pad) in pads) {
            if (key.dimension != level.dimension) continue
            if (pad.expires < now) { pads.remove(key, pad); feedback.remove(key); continue }
            hasPads = true
            val source = pad.ship?.let(level::getBodyById)
            if (pad.ship != null && source == null) { feedback.remove(key); continue }
            val n = Vector3d(pad.face.stepX.toDouble(), pad.face.stepY.toDouble(), pad.face.stepZ.toDouble())
            val local = Vector3d(key.pos.x + 0.5, key.pos.y + 0.5, key.pos.z + 0.5)
            val normal = source?.kinematics?.rotation?.transform(n, Vector3d()) ?: Vector3d(n)
            val side = if (pad.face.axis == Direction.Axis.X) Vector3d(0.0, 1.0, 0.0) else Vector3d(1.0, 0.0, 0.0)
            val other = n.cross(side, Vector3d())
            val samples = listOf(Vector3d(), Vector3d(side).mul(0.32), Vector3d(side).mul(-0.32),
                Vector3d(other).mul(0.32), Vector3d(other).mul(-0.32))
            var contact: Contact? = null
            for (offset in samples) {
                val point = Vector3d(local).add(offset)
                source?.kinematics?.toWorld?.transformPosition(point)
                val inset = if (source == null) -0.002 else 0.04
                val start = Vector3d(point).fma(-inset, normal)
                val range = if (grips.containsKey(key)) GripPadPhysics.RELEASE_RANGE else GripPadPhysics.CONTACT_RANGE
                val hit = level.rayCast(start, normal, range + inset, *listOfNotNull(pad.ship).toLongArray()) ?: continue
                if (hit.hitBody.id == pad.ship || hit.hitNormal.dot(normal) > -0.55) continue
                val hitPoint = Vector3d(start).fma(hit.distance, normal)
                val relative = velocity(source, hitPoint).sub(velocity(hit.hitBody, hitPoint))
                contact = Contact(key, pad, source, hit.hitBody, hitPoint, Vector3d(hit.hitNormal).normalize(), normal, relative,
                    maxOf(0.0, relative.dot(normal)))
                break
            }
            if (contact != null) contacts.add(contact)
            else feedback[key] = Feedback(false, false, 0.0, 0.0, 0.0, now + 1_000_000_000L)
        }
        val frame = readDiagnostics(level, hasPads)
        val normalLoads = frame?.takeUnless { it.overflow || contacts.isEmpty() }?.let { measureLoads(it, contacts) }
        val contactKeys = contacts.mapTo(HashSet()) { it.key }
        loads.keys.removeIf { it.dimension == level.dimension && (frame?.overflow == true || it !in contactKeys) }
        val active = contacts.filter { it.pad.powered && (it.source?.isStatic == false || !it.target.isStatic) }
        grips.keys.removeIf { key -> key.dimension == level.dimension && active.none { it.key == key } }
        feedback.keys.removeIf { it.dimension == level.dimension && !pads.containsKey(it) }
        if (contacts.isEmpty()) return
        val graph = ConnectedShipMass((level as VsiPhysLevel).getAllJoints().values) { id ->
            level.getBodyById(id)?.let { ConnectedShipMass.Load(it.inertiaData.mass, it.isStatic) }
        }
        for (contact in contacts) {
            val (key, pad, source, target, point, normal, _, relative, impact) = contact
            var gripping = false
            if (contact in active) {
                var grip = grips[key]
                if (grip == null || grip.source != pad.ship || grip.target != target.id || grip.face != pad.face) {
                    grip = Grip(pad.ship, target.id, target.kinematics.toModel.transformPosition(point, Vector3d()), pad.face)
                    grips[key] = grip
                }
                val anchor = target.kinematics.toWorld.transformPosition(grip.point, Vector3d())
                val error = GripPadPhysics.tangent(anchor.sub(point), normal)
                if (error.lengthSquared() > GripPadPhysics.MAX_SLIP * GripPadPhysics.MAX_SLIP) {
                    target.kinematics.toModel.transformPosition(point, grip.point)
                    error.zero()
                }
                val (a, b) = graph.endpoints(pad.ship, target.id)
                val mass = SpringJoints.effectiveMass(a.mass, b.mass, a.anchored, b.anchored)
                val mover = if (source?.isStatic == false) source.id else target.id
                val count = active.count {
                    val otherMover = if (it.source?.isStatic == false) it.source.id else it.target.id
                    graph.sameRigidComponent(mover, otherMover)
                }
                val inverse = inversePointMass(source, point) + inversePointMass(target, point)
                val force = GripPadPhysics.force(error, relative, normal, mass, inverse, count, delta)
                if (source?.isStatic == false) source.applyWorldForce(force, point)
                if (!target.isStatic) target.applyWorldForce(Vector3d(force).negate(), point)
                gripping = true
            }
            val normalLoad = normalLoad(level, contact, normalLoads)
            val slipSpeed = GripPadPhysics.tangent(relative, normal).length()
            val support = if (normalLoad != null && normalLoad > 1e-6) loads[key]?.let { load ->
                Support(pad.ship, target.id, normal.y, load.pointsInTarget.map { target.kinematics.toWorld.transformPosition(it, Vector3d()) })
            } else null
            feedback[key] = Feedback(true, gripping, impact, normalLoad, slipSpeed, now + 1_000_000_000L, support)
        }
    }

    private fun readDiagnostics(level: PhysLevel, hasPads: Boolean): ContactDiagnosticsFrame? {
        if (!hasPads) {
            diagnostics.remove(level)?.close()
            return null
        }
        if (level !in diagnostics) {
            diagnostics[level] = try {
                level.subscribeContactDiagnostics(ContactDiagnosticsOptions(collisions = true, maximumRecords = 4096))
            } catch (_: UnsupportedOperationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return diagnostics[level]?.read()
    }

    private fun measureLoads(frame: ContactDiagnosticsFrame, contacts: List<Contact>): Map<Key, MeasuredLoad>? {
        val duration = frame.endTime - frame.startTime
        if (duration <= 0.0 || !duration.isFinite()) return null
        val byBodies = contacts.groupBy { it.pad.body to it.target.id }
        val loads = HashMap<Key, MeasuredLoad>()
        for (reaction in frame.collisions) {
            val alongNormal = reaction.force.dot(reaction.normal)
            if (!alongNormal.isFinite() || alongNormal == 0.0 || !reaction.point.isFinite ||
                reaction.duration <= 0.0 || !reaction.duration.isFinite()) continue
            val normalOnBody0 = Vector3d(reaction.normal).mul(if (alongNormal > 0.0) 1.0 else -1.0)
            val average = abs(alongNormal) * reaction.duration / duration
            accumulateLoad(byBodies[reaction.body0 to reaction.body1], reaction.point, normalOnBody0, average, reaction.timestamp, loads)
            accumulateLoad(byBodies[reaction.body1 to reaction.body0], reaction.point, normalOnBody0.negate(), average, reaction.timestamp, loads)
        }
        return loads
    }

    private fun accumulateLoad(contacts: List<Contact>?, point: Vector3dc, normal: Vector3dc, load: Double, time: Double,
                               loads: MutableMap<Key, MeasuredLoad>) {
        if (contacts.isNullOrEmpty()) return
        val local = Vector3d(point)
        contacts.first().source?.kinematics?.toModel?.transformPosition(local)
        var nearest: Contact? = null
        var distance = Double.POSITIVE_INFINITY
        for (contact in contacts) {
            if (normal.dot(contact.facing) > -0.55) continue
            val score = GripPadPhysics.contactDistanceSquared(local, contact.key.pos, contact.pad.face) ?: continue
            if (score < distance) {
                nearest = contact
                distance = score
            }
        }
        nearest?.let { loads.getOrPut(it.key, ::MeasuredLoad).add(load, time,
            it.target.kinematics.toModel.transformPosition(point, Vector3d())) }
    }

    private fun normalLoad(level: PhysLevel, contact: Contact, measured: Map<Key, MeasuredLoad>?): Double? {
        if (measured == null) return null
        val (key, pad, source, target) = contact
        val fresh = measured[key]
        if (fresh != null) {
            return fresh.force.takeIf(Double::isFinite)?.also { loads[key] = Load(pad.body, target.id, pad.face, it, fresh.points) }
        }
        if ((source == null || source.isStatic || level.getShipById(source.id)?.isSleeping == true) &&
            (target.isStatic || level.getShipById(target.id)?.isSleeping == true) &&
            (source?.isStatic == false || !target.isStatic)) {
            return loads[key]?.takeIf { it.body == pad.body && it.target == target.id && it.face == pad.face }?.force
        }
        loads.remove(key)
        return 0.0
    }

    private fun velocity(body: PhysVsBody?, point: Vector3d): Vector3d = if (body == null) Vector3d() else
        body.angularVelocity.cross(Vector3d(point).sub(body.kinematics.position), Vector3d()).add(body.velocity)

    private fun inversePointMass(body: PhysVsBody?, point: Vector3d): Double {
        if (body == null || body.isStatic) return 0.0
        val mass = body.inertiaData.mass
        if (!mass.isFinite() || mass <= 0) return 0.0
        val inverse = Matrix3d(body.inertiaData.inertiaTensor).invert()
        val radius = point.distanceSquared(body.kinematics.position)
        return 1.0 / mass + radius * (inverse.m00() + inverse.m11() + inverse.m22())
    }
}
