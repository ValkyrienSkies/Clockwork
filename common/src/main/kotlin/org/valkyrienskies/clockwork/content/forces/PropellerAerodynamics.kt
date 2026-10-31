package org.valkyrienskies.clockwork.content.forces

import org.joml.Matrix4dc
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerTransform
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import kotlin.math.*

internal object PropellerAerodynamics {
    private const val EPSILON = 1e-12
    private const val PROFILE_DRAG = 0.01
    private val sailTrimPitch = Math.toRadians(4.0)
    private val stallStart = Math.toRadians(12.0)
    private val stallEnd = Math.toRadians(30.0)
    private val gaussNodes = doubleArrayOf(-1.0 / sqrt(3.0), 1.0 / sqrt(3.0))

    fun bearingSpeedToRadiansPerSecond(degreesPerTick: Double): Double = Math.toRadians(degreesPerTick) * 20.0

    data class Coefficients(val lift: Double, val drag: Double)

    fun coefficients(angleOfAttack: Double): Coefficients {
        if (!angleOfAttack.isFinite()) return Coefficients(0.0, 0.0)
        val alpha = Math.IEEEremainder(angleOfAttack, PI)
        val transition = ((abs(alpha) - stallStart) / (stallEnd - stallStart)).coerceIn(0.0, 1.0)
        val blend = transition * transition * (3.0 - 2.0 * transition)
        val attachedLift = 2.0 * PI * alpha
        val separatedLift = sin(2.0 * alpha)
        val attachedDrag = PROFILE_DRAG + 0.01 * attachedLift * attachedLift
        val separatedDrag = PROFILE_DRAG + 2.0 * sin(alpha).pow(2)
        return Coefficients(
            attachedLift * (1.0 - blend) + separatedLift * blend,
            attachedDrag * (1.0 - blend) + separatedDrag * blend
        )
    }

    /**
     * [velocityThroughAir] is blade velocity minus air velocity in world coordinates.
     */
    fun sectionForce(
        velocityThroughAir: Vector3dc, span: Vector3dc, chord: Vector3dc, density: Double
    ): Vector3d = sectionResponse(velocityThroughAir, span, chord, density)
        .transform(velocityThroughAir, Vector3d()).let { if (it.isFinite) it else Vector3d() }

    /** Freeze the polar and speed, but retain the force's dependence on velocity.
     * Its symmetric part is drag (negative semidefinite); lift is skew-symmetric.
     */
    private fun sectionResponse(
        velocityThroughAir: Vector3dc, span: Vector3dc, chord: Vector3dc, density: Double
    ): Matrix3d {
        val response = Matrix3d().zero()
        if (!velocityThroughAir.isFinite || !span.isFinite || !chord.isFinite ||
            !density.isFinite() || density <= 0.0 || span.lengthSquared() < EPSILON) return response

        val normal = span.cross(chord, Vector3d())
        val area = normal.length()
        if (!area.isFinite() || area < EPSILON) return response
        normal.div(area)
        val spanUnit = Vector3d(span).normalize()
        val chordUnit = normal.cross(spanUnit, Vector3d())
        val spanSpeed = velocityThroughAir.dot(spanUnit)
        val sectionVelocity = Vector3d(velocityThroughAir).sub(Vector3d(spanUnit).mul(spanSpeed))
        val speedSquared = sectionVelocity.lengthSquared()
        if (speedSquared > EPSILON) {
            val alpha = atan2(-sectionVelocity.dot(normal), sectionVelocity.dot(chordUnit))
            val polar = coefficients(alpha)
            val pressureAreaPerSpeed = 0.5 * density * sqrt(speedSquared) * area
            for (column in 0..2) {
                val basis = Vector3d().setComponent(column, 1.0)
                val projected = Vector3d(basis).sub(Vector3d(spanUnit).mul(spanUnit.get(column)))
                response.setColumn(column, spanUnit.cross(basis, Vector3d()).mul(polar.lift)
                    .sub(projected.mul(polar.drag)).mul(pressureAreaPerSpeed))
            }
        }
        val spanDrag = 0.5 * density * area * PROFILE_DRAG * abs(spanSpeed)
        for (column in 0..2) response.setColumn(column, response.getColumn(column, Vector3d())
            .sub(Vector3d(spanUnit).mul(spanDrag * spanUnit.get(column))))
        return if (response.isFinite) response else Matrix3d().zero()
    }

    data class Flow(
        val modelToWorld: Matrix4dc,
        val centerOfMassModel: Vector3dc,
        val velocityWorld: Vector3dc,
        val angularVelocityWorld: Vector3dc,
        val windWorld: Vector3dc,
        val densityAtHeight: (Double) -> Double
    )

    private data class Element(
        val offset: Vector3d,
        val span: Vector3d,
        val tangent: Vector3d,
        val spanLength: Double,
        val chordLength: Double,
        val pitch: Double
    )

    data class Section(val lever: Vector3dc, val response: Matrix3d)

    data class PreparedRotor(
        val force: Vector3dc = Vector3d(), val torque: Vector3dc = Vector3d(),
        val sections: List<Section> = emptyList()
    )

    fun compute(
        prop: PropData, flow: Flow, angleDegrees: Double, forceMultiplier: Double,
        maxForce: Double, maxTorque: Double, radialSegments: Int = 8
    ): Pair<Vector3dc, Vector3dc> = prepare(prop, flow, angleDegrees, forceMultiplier, maxForce, maxTorque, radialSegments)
        .let { it.force to it.torque }

    fun prepare(
        prop: PropData, flow: Flow, angleDegrees: Double, forceMultiplier: Double,
        maxForce: Double, maxTorque: Double, radialSegments: Int = 8
    ): PreparedRotor {
        val force = Vector3d()
        val torque = Vector3d()
        val position = prop.position ?: return PreparedRotor()
        val baseAxis = Vector3d(prop.bearingAxis ?: return PreparedRotor())
        if (!baseAxis.isFinite || baseAxis.lengthSquared() < EPSILON || !angleDegrees.isFinite() ||
            !prop.bearingSpeed.isFinite() || !forceMultiplier.isFinite() || forceMultiplier <= 0.0 ||
            !flow.modelToWorld.isFinite || !flow.centerOfMassModel.isFinite || !flow.velocityWorld.isFinite ||
            !flow.angularVelocityWorld.isFinite || !flow.windWorld.isFinite) return PreparedRotor()
        baseAxis.normalize()
        val facingSign = PropellerTransform.facingSign(baseAxis)
        val tilt = prop.bearingTiltQuat?.let { Quaterniond(it) } ?: run {
            val tiltedAxis = prop.bearingAxisRot ?: baseAxis
            if (!tiltedAxis.isFinite || tiltedAxis.lengthSquared() < EPSILON) return PreparedRotor()
            Quaterniond().rotationTo(baseAxis, Vector3d(tiltedAxis).normalize())
        }
        if (!tilt.isFinite || tilt.lengthSquared() < EPSILON) return PreparedRotor()
        tilt.normalize()
        val orientation = PropellerTransform.orientation(baseAxis, tilt, angleDegrees)
        val axis = tilt.transform(baseAxis, Vector3d())
        val omega = bearingSpeedToRadiansPerSecond(prop.bearingSpeed) * facingSign
        val hub = Vector3d(position).add(0.5, 0.5, 0.5).add(baseAxis)
            .add(PropellerTransform.hubDisplacement(baseAxis, tilt))
        val elements = ArrayList<Element>()

        if (prop.brass) {
            for (pos in prop.sailPositions.orEmpty()) {
                val offset = orientation.transform(Vector3d(pos))
                val span = Vector3d(offset).sub(Vector3d(axis).mul(offset.dot(axis)))
                if (span.lengthSquared() < EPSILON) span.set(perpendicular(axis)) else span.normalize()
                elements.add(Element(offset, span, axis.cross(span, Vector3d()), 1.0, 1.0, 0.0))
            }
        } else {
            val count = prop.blades.size
            val initialSpan = when {
                baseAxis.y > 0.9 -> Vector3d(0.0, 0.0, -1.0)
                baseAxis.y < -0.9 -> Vector3d(0.0, 0.0, 1.0)
                else -> Vector3d(0.0, -1.0, 0.0)
            }
            val segments = radialSegments.coerceIn(1, 64)
            for ((index, blade) in prop.blades.withIndex()) {
                if (!blade.length.isFinite() || blade.length <= 0.0 || !blade.angle.isFinite()) continue
                val span = Vector3d(initialSpan).rotateAxis(2.0 * PI * index / count, baseAxis.x, baseAxis.y, baseAxis.z)
                orientation.transform(span)
                val tangent = axis.cross(span, Vector3d())
                val segmentLength = blade.length / segments
                for (segment in 0 until segments) {
                    for (node in gaussNodes) {
                        val radius = (segment + 0.5 + 0.5 * node) * segmentLength
                        elements.add(Element(
                            Vector3d(span).mul(radius), span, tangent, segmentLength / 2.0,
                            if (blade.wide) 0.375 else 0.25, -Math.toRadians(blade.angle)
                        ))
                    }
                }
            }
        }

        fun velocityAt(offset: Vector3dc): Vector3d {
            val leverWorld = flow.modelToWorld.transformDirection(Vector3d(hub).add(offset).sub(flow.centerOfMassModel))
            val spinVelocity = flow.modelToWorld.transformDirection(axis.cross(offset, Vector3d()).mul(omega))
            return Vector3d(flow.velocityWorld).sub(flow.windWorld)
                .add(flow.angularVelocityWorld.cross(leverWorld, Vector3d())).add(spinVelocity)
        }

        val densities = DoubleArray(elements.size) { index ->
            val pointWorld = flow.modelToWorld.transformPosition(Vector3d(hub).add(elements[index].offset))
            flow.densityAtHeight(pointWorld.y)
        }

        // Store collective in the normal rotation setting's handedness. Mirroring only
        // the physical pitch keeps startup and live setting changes consistent too.
        val sailPitchSign = if (prop.inverted) -1.0 else 1.0
        if (prop.brass && elements.isNotEmpty()) {
            val rotationSign = if (omega < 0.0) -1.0 else 1.0
            var inflow = 0.0
            var totalWeight = 0.0
            for ((index, element) in elements.withIndex()) {
                val velocity = velocityAt(element.offset)
                val spanWorld = flow.modelToWorld.transformDirection(element.span, Vector3d())
                val tangentWorld = flow.modelToWorld.transformDirection(element.tangent, Vector3d())
                val area = spanWorld.cross(tangentWorld, Vector3d()).length()
                spanWorld.normalize()
                tangentWorld.sub(Vector3d(spanWorld).mul(tangentWorld.dot(spanWorld))).normalize()
                val normalWorld = spanWorld.cross(tangentWorld, Vector3d())
                val tangentSpeed = abs(velocity.dot(tangentWorld))
                val axialSpeed = velocity.dot(normalWorld) * rotationSign * sailPitchSign
                // Axial lift sensitivity: dynamic pressure times the lift's axial projection.
                // Equal weighting lets slow inner sails over-pitch the stronger outer sails.
                val weight = densities[index] * area * tangentSpeed * hypot(tangentSpeed, axialSpeed)
                if (weight.isFinite() && weight > 0.0) {
                    inflow += weight * atan2(axialSpeed, tangentSpeed)
                    totalWeight += weight
                }
            }
            val meanInflow = if (totalWeight > EPSILON && totalWeight.isFinite() && inflow.isFinite()) inflow / totalWeight else 0.0
            // Retain damping near hover while continuing to follow airflow at cruise.
            // The cosine compensates for increasing section airspeed; the smooth trim
            // reduction retains 75% of the lift target in forward flight, without a pitch cap.
            val trim = sailTrimPitch * cos(meanInflow) * (1.0 - 0.25 * tanh(meanInflow / (2.0 * sailTrimPitch)))
            val targetPitch = meanInflow + trim
            // One collective update per rotor, independent of sail count and iteration order.
            if (!prop.currentBladePitch.isFinite()) prop.currentBladePitch = sailTrimPitch
            prop.currentBladePitch += 0.05 * (targetPitch - prop.currentBladePitch)
        }

        val sections = ArrayList<Section>(elements.size)
        for ((index, element) in elements.withIndex()) {
            val pitch = if (prop.brass) sailPitchSign * prop.currentBladePitch else element.pitch
            val spanWorld = flow.modelToWorld.transformDirection(Vector3d(element.span).mul(element.spanLength))
            val chordWorld = flow.modelToWorld.transformDirection(
                Vector3d(element.tangent).mul(cos(pitch)).add(Vector3d(axis).mul(sin(pitch))).mul(element.chordLength)
            )
            val pointModel = Vector3d(hub).add(element.offset)
            val velocity = velocityAt(element.offset)
            val response = sectionResponse(velocity, spanWorld, chordWorld, densities[index])
                .scale(forceMultiplier)
            val elementForce = response.transform(velocity, Vector3d())
            val leverWorld = flow.modelToWorld.transformDirection(pointModel.sub(flow.centerOfMassModel))
            sections.add(Section(leverWorld, response))
            force.add(elementForce)
            torque.add(leverWorld.cross(elementForce, Vector3d()))
        }

        if (!force.isFinite || !torque.isFinite) return PreparedRotor()
        var fraction = 1.0
        if (maxForce > 0.0 && force.length() > maxForce) fraction = min(fraction, maxForce / force.length())
        if (maxTorque > 0.0 && torque.length() > maxTorque) fraction = min(fraction, maxTorque / torque.length())
        if (fraction < 1.0) sections.forEach { it.response.scale(fraction) }
        return PreparedRotor(force.mul(fraction), torque.mul(fraction), sections)
    }

    private fun perpendicular(axis: Vector3dc): Vector3d {
        val candidate = if (abs(axis.y()) < 0.9) Vector3d(0.0, 1.0, 0.0) else Vector3d(0.0, 0.0, -1.0)
        return candidate.sub(Vector3d(axis).mul(candidate.dot(axis))).normalize()
    }
}
