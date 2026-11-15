package org.valkyrienskies.clockwork.content.physicalities.wheel

import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Quaterniond
import org.valkyrienskies.core.api.bodies.PhysVsBody
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

internal class WheelStepAssist {
    data class Settings(val height: Double, val strength: Double, val maximumForce: Double)
    data class Hit(val body: Long, val distance: Double, val normal: Vector3dc, val velocity: Vector3dc, val friction: Double = .8)
    data class Push(val surface: Long, val force: Vector3d)
    data class Step(val surface: Long, val edge: Vector3d, val forward: Vector3d, val up: Vector3d,
                    val height: Double, val run: Double, val rideHeight: Double, val friction: Double = .8)

    private var bodyId = -1L
    private var sequence = -1L
    private var staleTicks = 0
    private var nextProbe = 0.0
    private var nextGraph = 0.0
    private var excluded = longArrayOf()
    private var step: Step? = null
    private var carriedLoad = 0.0
    private var filteredLoad = 0.0
    private var previousTime = 0.0
    private val probePosition = Vector3d()

    fun apply(level: VsiPhysLevel, wheel: PhysVsBody, tire: WheelTire, rpm: Float?, torque: Double, settings: Settings) {
        if (wheel.isStatic || settings.strength <= 0.0) { step = null; return }
        val frame = WheelContacts.lastFrame(level.dimension) ?: return
        if (frame.sequence == sequence) staleTicks++ else { sequence = frame.sequence; staleTicks = 0 }
        if (frame.reset || frame.overflow || staleTicks > 2) { step = null; return }
        val time = frame.endTime
        if (wheel.id != bodyId || time < previousTime || time - previousTime > .25) {
            bodyId = wheel.id; step = null; nextProbe = 0.0; nextGraph = 0.0; carriedLoad = 0.0; filteredLoad = 0.0
        }
        val dt = (time - previousTime).coerceIn(0.0, .05)
        previousTime = time
        val gravity = level.getGravity()
        val gravityLength = gravity.length()
        if (!gravity.isFinite || !gravityLength.isFinite() || gravityLength < 1e-4) { step = null; return }
        val up = Vector3d(gravity).div(-gravityLength)
        val support = WheelContacts.support(level.dimension, wheel.id)
        val load = support?.takeIf { it.position.distanceSquared(wheel.kinematics.position) < 1.0 }
            ?.force?.dot(up)?.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        filteredLoad = if (filteredLoad < 100.0 && load > 100.0) load else filteredLoad + (load - filteredLoad) * (dt * 6.0).coerceAtMost(1.0)
        if (step == null && filteredLoad > 100.0) carriedLoad = filteredLoad
        if (rpm == 0f || carriedLoad < 100.0) { step = null; return }
        val axle = wheel.kinematics.rotation.transform(Vector3d(1.0, 0.0, 0.0))
        if (abs(axle.dot(up)) > .5) { step = null; return }
        val center = wheel.kinematics.position
        val scale = abs(wheel.kinematics.scaling.x())
        val radius = tire.radius * scale
        val halfWidth = tire.width * scale / 2
        if (step == null && rpm == null && wheel.velocity.lengthSquared() < .04) return
        if (time >= nextProbe || probePosition.distanceSquared(center) > .25) {
            nextProbe = time + .1
            probePosition.set(center)
            if (time >= nextGraph) {
                val ignored = linkedSetOf(wheel.id)
                val pending = ArrayDeque<Long>()
                pending.add(wheel.id)
                while (pending.isNotEmpty()) {
                    for (id in level.getJointsFromShip(pending.removeFirst())) {
                        val joint = level.getJointById(id) ?: continue
                        fun add(body: Long?) {
                            if (body != null && body !in ignored && level.getBodyById(body)?.isStatic == false) {
                                ignored.add(body); pending.add(body)
                            }
                        }
                        add(joint.shipId0); add(joint.shipId1)
                        if (ignored.size > 128) { step = null; return }
                    }
                }
                excluded = ignored.toLongArray()
                nextGraph = time + .5
            }
            val candidate = probe(center, axle, up, wheel.velocity, radius, halfWidth, rpm?.toDouble(), settings) { start, direction, length ->
                level.rayCast(start, direction, length, *excluded)?.let { hit ->
                    val point = Vector3d(start).fma(hit.distance, direction)
                    val velocity = Vector3d(hit.angularVelocity).cross(Vector3d(center).sub(point)).add(hit.velocity)
                    Hit(hit.hitBody.id, hit.distance, hit.hitNormal, velocity, (hit.dynamicFrictionCoefficient.toDouble() + 1.4) * .5 * .75)
                }
            }
            if (candidate != null) {
                val surface = level.getBodyById(candidate.surface) ?: return
                val old = step?.takeIf { it.surface == candidate.surface }?.let { worldStep(it, surface) }
                val continued = old != null && abs(Vector3d(candidate.edge).sub(old.edge).dot(up)) < .15 && old.forward.dot(candidate.forward) > .8
                val next = if (continued) candidate.copy(rideHeight = old!!.rideHeight, run = maxOf(old.run, candidate.run)) else candidate
                step = localStep(next, surface)
            } else {
                step = null
            }
        }
        val cached = step ?: return
        val surface = level.getBodyById(cached.surface) ?: run { step = null; return }
        val current = worldStep(cached, surface)
        val surfaceVelocity = Vector3d(surface.angularVelocity).cross(Vector3d(center).sub(surface.kinematics.position)).add(surface.velocity)
        val relative = Vector3d(wheel.velocity).sub(surfaceVelocity)
        val supportedLoad = maxOf(carriedLoad, filteredLoad).coerceAtMost(carriedLoad * 2.0)
        val push = force(center, axle, relative, radius, rpm?.toDouble(), torque, supportedLoad, settings, current, gravityLength) ?: return
        wheel.applyWorldForce(push.force, center)
        if (!surface.isStatic) surface.applyWorldForce(Vector3d(push.force).negate(), center)
        val normalLoad = push.force.length()
        val normal = Vector3d(push.force).div(normalLoad)
        val tangent = Vector3d(axle).cross(normal).normalize()
        val arm = Vector3d(normal).mul(-radius)
        val contactVelocity = Vector3d(wheel.angularVelocity).sub(surface.angularVelocity).cross(arm).add(relative)
        val grip = traction(contactVelocity.dot(tangent), normalLoad, current.friction,
            wheel.inertiaData.mass, wheel.inertiaData.inertiaTensor.m00(), radius, dt, settings.maximumForce)
        if (grip != 0.0) {
            val point = Vector3d(center).add(arm)
            val force = tangent.mul(grip)
            wheel.applyWorldForce(force, point)
            if (!surface.isStatic) surface.applyWorldForce(Vector3d(force).negate(), point)
        }
    }

    private fun localStep(step: Step, body: PhysVsBody) = step.copy(
        edge = body.kinematics.toModel.transformPosition(step.edge, Vector3d()),
        forward = body.kinematics.rotation.conjugate(Quaterniond()).transform(step.forward, Vector3d()),
        up = body.kinematics.rotation.conjugate(Quaterniond()).transform(step.up, Vector3d()))

    private fun worldStep(step: Step, body: PhysVsBody) = step.copy(
        edge = body.kinematics.toWorld.transformPosition(step.edge, Vector3d()),
        forward = body.kinematics.rotation.transform(step.forward, Vector3d()),
        up = body.kinematics.rotation.transform(step.up, Vector3d()))

    companion object {
        internal fun probe(center: Vector3dc, axle: Vector3dc, up: Vector3dc, velocity: Vector3dc,
                           radius: Double, halfWidth: Double, rpm: Double?, settings: Settings,
                           cast: (Vector3dc, Vector3dc, Double) -> Hit?): Step? {
            if (!center.isFinite || !axle.isFinite || !up.isFinite || !velocity.isFinite ||
                !radius.isFinite() || radius <= 0.0 || !halfWidth.isFinite() || halfWidth <= 0.0 ||
                !settings.height.isFinite() || settings.height <= 0.0 || abs(axle.dot(up)) > .5 ||
                rpm == 0.0 || rpm?.isFinite() == false) return null
            val heightLimit = minOf(settings.height, radius)
            val down = Vector3d(up).negate()
            val support = cast(center, down, radius + heightLimit + .15) ?: return null
            if (support.normal.dot(up) < .6 || support.distance < radius * .15) return null
            val relative = Vector3d(velocity).sub(support.velocity)
            if (relative.dot(up) < -1.5) return null
            val forward = Vector3d(axle).cross(up).normalize()
            val rollingSpeed = relative.dot(forward)
            val direction = if (rpm != null) sign(rpm) else if (abs(rollingSpeed) > .2) sign(rollingSpeed) else 0.0
            if (direction == 0.0 || rollingSpeed * direction < -1.0) return null
            forward.mul(direction)
            val base = Vector3d(center).fma(-support.distance, up)
            val toe = minOf(.15, radius * .15)
            val start = Vector3d(base).fma(toe, up)
            val reach = radius * 2.2 + minOf(radius, abs(rollingSpeed) * .25)
            val wall = cast(start, forward, reach) ?: return null
            if (wall.normal.dot(forward) > -.4 || abs(wall.normal.dot(up)) > .5 || wall.distance <= .02) return null
            val inset = .08 + halfWidth * abs(wall.normal.dot(axle)) / -wall.normal.dot(forward)
            val topStart = Vector3d(start).fma(wall.distance + inset, forward).fma(heightLimit + .1 - toe, up)
            var height = 0.0
            var friction = Double.POSITIVE_INFINITY
            var supported = 0
            for (side in -1..1) {
                val sample = Vector3d(topStart).fma(side * .9 * halfWidth, axle)
                sample.fma(-side * .9 * halfWidth * axle.dot(up), up)
                val top = cast(sample, down, heightLimit + .1)
                if (top == null) { if (side == 0) return null else continue }
                val rise = heightLimit + .1 - top.distance
                if (top.body != wall.body || top.normal.dot(up) < .7 || rise < .08 || rise > heightLimit + 1e-6) {
                    if (side == 0) return null else continue
                }
                height = maxOf(height, rise)
                if (!top.friction.isFinite()) return null
                friction = minOf(friction, top.friction.coerceIn(0.0, 2.0))
                supported++
            }
            if (supported < 2) return null
            if (cast(Vector3d(topStart).fma(height - heightLimit - .06, up), up, radius * 2.0 + .08) != null ||
                cast(Vector3d(center).fma(radius, up), up, height + .1) != null) return null
            return Step(wall.body, Vector3d(base).fma(wall.distance, forward).fma(height, up), forward, Vector3d(up),
                height, reach, minOf(support.distance, radius - .02), friction)
        }

        internal fun traction(slip: Double, load: Double, friction: Double, mass: Double, axialInertia: Double, radius: Double, dt: Double,
                              maximumForce: Double = Double.POSITIVE_INFINITY): Double {
            if (!slip.isFinite() || !load.isFinite() || !friction.isFinite() || !mass.isFinite() || !axialInertia.isFinite() ||
                !radius.isFinite() || !dt.isFinite() || mass <= 0.0 || axialInertia <= 0.0 || radius <= 0.0 || dt <= 0.0) return 0.0
            if (maximumForce.isNaN() || maximumForce <= 0.0) return 0.0
            val effectiveMass = 1.0 / (1.0 / mass + radius * radius / axialInertia)
            val remaining = sqrt((maximumForce * maximumForce - load * load).coerceAtLeast(0.0))
            val limit = (load * friction).coerceIn(0.0, remaining)
            return (-slip * effectiveMass / maxOf(dt, 1.0 / 120.0)).coerceIn(-limit, limit)
        }

        internal fun force(center: Vector3dc, axle: Vector3dc, relative: Vector3dc, radius: Double,
                           rpm: Double?, torque: Double, load: Double, settings: Settings, step: Step, gravity: Double = 10.0): Push? {
            if (!load.isFinite() || load <= 0.0 || !torque.isFinite() || torque <= 0.0 ||
                !settings.strength.isFinite() || settings.strength <= 0.0 || !settings.maximumForce.isFinite() || settings.maximumForce <= 0.0 ||
                rpm == 0.0 || !relative.isFinite || !center.isFinite || relative.dot(step.up) < -1.5) return null
            val direction = Vector3d(axle).cross(step.up).normalize()
            if (abs(direction.dot(step.forward)) < .8) return null
            val offset = Vector3d(center).sub(step.edge)
            val distance = -offset.dot(step.forward)
            val sideways = Vector3d(step.up).cross(step.forward).normalize()
            if (distance < -.1 || distance > step.run || abs(offset.dot(sideways)) > .6) return null
            val speed = relative.dot(step.forward)
            if (speed < -1.0 || rpm == null && speed < .2) return null
            val progress = (1.0 - distance / step.run).coerceIn(0.0, 1.0)
            var rise = step.height * progress * progress * (3.0 - 2.0 * progress)
            var slope = step.height * 6.0 * progress * (1.0 - progress) / step.run
            if (distance in 0.0..radius) {
                val cornerHeight = sqrt((radius * radius - distance * distance).coerceAtLeast(0.0))
                val envelopeRise = step.height + cornerHeight - .02 - step.rideHeight
                if (envelopeRise > rise) {
                    rise = envelopeRise
                    slope = distance / maxOf(.05, cornerHeight)
                }
            }
            val error = step.rideHeight + rise - (offset.dot(step.up) + step.height)
            if (error < -.15) return null
            val targetSpeed = (maxOf(0.0, speed) * slope).coerceAtMost(1.5)
            val engagement = (progress * 4.0).coerceIn(0.0, 1.0)
            val demand = load * engagement * engagement * (3.0 - 2.0 * engagement) +
                load / gravity * (64.0 * error + 12.0 * (targetSpeed - relative.dot(step.up)))
            val normal = Vector3d(step.up)
            val reserve = minOf(load * settings.strength.coerceAtMost(1.0), torque / (radius * maxOf(.2, slope)))
            val limit = minOf(settings.maximumForce, load + reserve)
            val lift = demand.coerceIn(0.0, limit) * ((1.5 - relative.dot(step.up)) / .4).coerceIn(0.0, 1.0)
            return if (lift > 0.0) Push(step.surface, normal.mul(lift)) else null
        }

    }
}
