package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.sqrt

/** Positions and forces are in ship model space. Opposing forces keep their own weight. */
data class AeronautForceCenter(val position: Vector3dc, val force: Vector3dc, val weight: Double)

class AeronautForceAccumulator {
    private val moment = Vector3d()
    private val resultant = Vector3d()
    private var weight = 0.0

    fun add(position: Vector3dc, force: Vector3dc) {
        val magnitude = force.length()
        if (!position.isFinite || !force.isFinite || !magnitude.isFinite() || magnitude <= 1e-8) return
        moment.fma(magnitude, position)
        resultant.add(force)
        weight += magnitude
    }

    fun add(center: AeronautForceCenter?) {
        if (center == null || !center.weight.isFinite() || center.weight <= 1e-8 ||
            !center.position.isFinite || !center.force.isFinite) return
        moment.fma(center.weight, center.position)
        resultant.add(center.force)
        weight += center.weight
    }

    fun center(): AeronautForceCenter? = if (weight > 1e-8 && weight.isFinite() && moment.isFinite && resultant.isFinite)
        AeronautForceCenter(Vector3d(moment).div(weight), Vector3d(resultant), weight) else null
}

/** Shared reference conditions make a stationary build's balance comparable between samples. */
object AeronautForceReadout {
    const val REFERENCE_RPM = 128.0
    const val REFERENCE_AIRSPEED = 10.0
    const val MAX_RANGE = 128.0
    const val UPDATE_TICKS = 5L

    // Same lift polar as VS wings. This is an airflow estimate, not a second physics simulation.
    fun wingLift(normal: Vector3dc, velocity: Vector3dc, density: Double, power: Double, camber: Double): Vector3d {
        if (!normal.isFinite || !velocity.isFinite || normal.lengthSquared() < 1e-12 ||
            !density.isFinite() || density <= 0.0 || !power.isFinite() || power <= 0.0 || !camber.isFinite()) return Vector3d()
        val n = Vector3d(normal).normalize()
        val normalSpeed = n.dot(velocity)
        val tangentSpeedSquared = Vector3d(velocity).fma(-normalSpeed, n).lengthSquared()
        if (tangentSpeedSquared < 1e-12) return Vector3d()
        val alpha = atan2(-normalSpeed, sqrt(tangentSpeedSquared)) + camber
        val magnitude = (power * sin(2.0 * alpha) * density * tangentSpeedSquared).coerceIn(-1e7, 1e7)
        val flow = Vector3d(velocity).normalize()
        return n.fma(-n.dot(flow), flow).normalize().mul(magnitude)
    }

    fun referenceFlow(normal: Vector3dc): Vector3d {
        val tangent = if (abs(normal.z()) < 0.9) Vector3d(0.0, 0.0, 1.0) else Vector3d(1.0, 0.0, 0.0)
        return tangent.fma(-tangent.dot(normal), normal).normalize().mul(REFERENCE_AIRSPEED)
    }
}
