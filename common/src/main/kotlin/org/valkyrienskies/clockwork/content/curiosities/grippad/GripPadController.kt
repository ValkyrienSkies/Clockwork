package org.valkyrienskies.clockwork.content.curiosities.grippad

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Matrix3d
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.spring.SpringJoints
import org.valkyrienskies.clockwork.util.ConnectedShipMass
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import java.util.concurrent.ConcurrentHashMap

internal object GripPadController {
    data class Key(val dimension: String, val pos: BlockPos)
    data class Pad(val ship: Long?, val face: Direction, val powered: Boolean, val expires: Long)
    data class Feedback(val contact: Boolean, val gripping: Boolean, val impact: Double, val expires: Long)
    private data class Grip(val source: Long?, val target: Long, val point: Vector3d, val face: Direction)
    private data class Contact(val key: Key, val pad: Pad, val source: PhysVsBody?, val target: PhysVsBody,
                               val point: Vector3d, val normal: Vector3d, val velocity: Vector3d, val impact: Double)
    private val pads = ConcurrentHashMap<Key, Pad>()
    private val feedback = ConcurrentHashMap<Key, Feedback>()
    private val grips = HashMap<Key, Grip>()

    fun publish(key: Key, ship: Long?, face: Direction, powered: Boolean) {
        pads[key] = Pad(ship, face, powered, System.nanoTime() + 1_000_000_000L)
    }

    fun remove(key: Key) { pads.remove(key); feedback.remove(key) }
    fun feedback(key: Key) = feedback[key]?.takeIf { it.expires > System.nanoTime() }

    fun tick(level: PhysLevel, delta: Double) {
        val now = System.nanoTime()
        val contacts = ArrayList<Contact>()
        for ((key, pad) in pads) {
            if (key.dimension != level.dimension) continue
            if (pad.expires < now) { pads.remove(key, pad); feedback.remove(key); continue }
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
                contact = Contact(key, pad, source, hit.hitBody, hitPoint, Vector3d(hit.hitNormal).normalize(), relative,
                    maxOf(0.0, relative.dot(normal)))
                break
            }
            if (contact != null) contacts.add(contact)
            else feedback[key] = Feedback(false, false, 0.0, now + 1_000_000_000L)
        }
        val active = contacts.filter { it.pad.powered && (it.source?.isStatic == false || !it.target.isStatic) }
        grips.keys.removeIf { key -> key.dimension == level.dimension && active.none { it.key == key } }
        feedback.keys.removeIf { it.dimension == level.dimension && !pads.containsKey(it) }
        if (contacts.isEmpty()) return
        val graph = ConnectedShipMass((level as VsiPhysLevel).getAllJoints().values) { id ->
            level.getBodyById(id)?.let { ConnectedShipMass.Load(it.inertiaData.mass, it.isStatic) }
        }
        for (contact in contacts) {
            val (key, pad, source, target, point, normal, relative, impact) = contact
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
            feedback[key] = Feedback(true, gripping, impact, now + 1_000_000_000L)
        }
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
