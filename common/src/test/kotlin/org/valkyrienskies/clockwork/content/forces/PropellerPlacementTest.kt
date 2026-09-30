package org.valkyrienskies.clockwork.content.forces

import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerTransform
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class PropellerPlacementTest {
    private val axes = listOf(
        Vector3d(1.0, 0.0, 0.0), Vector3d(-1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0),
        Vector3d(0.0, -1.0, 0.0), Vector3d(0.0, 0.0, 1.0), Vector3d(0.0, 0.0, -1.0)
    )
    private val tilt = Quaterniond().rotateX(0.4).rotateY(-0.3).rotateZ(0.2)

    @Test
    fun `shared transform matches the existing rendered pivot and spin for every facing`() {
        for (axis in axes) for (angle in listOf(0.0, 45.0, 90.0, 180.0, 270.0)) {
            val local = Vector3d(2.0, -1.0, 3.0)
            val transformed = PropellerTransform.orientation(axis, tilt, angle).transform(local, Vector3d())
                .add(PropellerTransform.hubDisplacement(axis, tilt))
            assertVector(renderedTransform(axis, angle).transformPosition(local, Vector3d()), transformed)

            val radial = radial(axis)
            val rotated = PropellerTransform.orientation(axis, tilt, angle).transform(radial, Vector3d())
            assertEquals(0.0, rotated.dot(tilt.transform(axis, Vector3d())), 1e-12)
            assertEquals(radial.length(), rotated.length(), 1e-12)
        }
    }

    @Test
    fun `moving sails along the shaft has no force jump or one sided cutoff`() {
        for (axis in axes) for (angle in listOf(0.0, 90.0, 180.0)) {
            val reference = solve(prop(axis, radial(axis)), angle = angle)
            val tiltedAxis = tilt.transform(axis, Vector3d())
            for (offset in listOf(-5, -4, -1, 0, 1, 4, 5)) {
                val result = solve(prop(axis, radial(axis).add(Vector3d(axis).mul(offset.toDouble()))), angle = angle)
                assertVector(reference.first, result.first)
                val addedMoment = Vector3d(tiltedAxis).mul(offset.toDouble()).cross(reference.first)
                assertVector(Vector3d(reference.second).add(addedMoment), result.second)
            }
        }
    }

    @Test
    fun `sail airflow density and moment use its rendered position under ship scale and tilt`() {
        val modelToWorld = Matrix4d().translation(8.0, 70.0, -3.0).rotateY(0.7).scale(1.3, 0.8, 1.7)
        val com = Vector3d(0.2, -0.4, 0.7)
        val velocity = Vector3d(3.0, -2.0, 1.0)
        val wind = Vector3d(-1.0, 0.7, 2.0)
        val shipOmega = Vector3d(0.4, -0.2, 0.3)
        for (axis in axes) for (angle in listOf(0.0, 90.0, 210.0)) {
            val local = radial(axis).add(Vector3d(axis).mul(2.0))
            val prop = prop(axis, local)
            var sampledHeight = Double.NaN
            val flow = PropellerAerodynamics.Flow(modelToWorld, com, velocity, shipOmega, wind) {
                sampledHeight = it
                1.225
            }
            val result = solve(prop, flow, angle)

            // Independent reconstruction of the original renderer's translation/rotation sequence.
            val rendered = renderedTransform(axis, angle)
            val point = rendered.transformPosition(local, Vector3d()).add(axis).add(0.5, 0.5, 0.5)
            val worldPoint = modelToWorld.transformPosition(point, Vector3d())
            assertEquals(worldPoint.y, sampledHeight, 1e-10)
            val offset = rendered.transformDirection(local, Vector3d())
            val normal = tilt.transform(axis, Vector3d())
            val span = rendered.transformDirection(radial(axis).normalize(), Vector3d())
            val tangent = normal.cross(span, Vector3d())
            val omega = 2.0 * Math.PI * if (axis.x + axis.y + axis.z < 0.0) -1.0 else 1.0
            val lever = modelToWorld.transformDirection(point.sub(com), Vector3d())
            val throughAir = Vector3d(velocity).sub(wind).add(shipOmega.cross(lever, Vector3d()))
                .add(modelToWorld.transformDirection(normal.cross(offset, Vector3d()).mul(omega)))
            val chord = Vector3d(tangent).mul(cos(prop.currentBladePitch))
                .add(Vector3d(normal).mul(sin(prop.currentBladePitch)))
            val expectedForce = PropellerAerodynamics.sectionForce(
                throughAir, modelToWorld.transformDirection(span), modelToWorld.transformDirection(chord), 1.225
            )
            assertVector(expectedForce, result.first)
            assertVector(lever.cross(expectedForce), result.second)
        }
    }

    private fun renderedTransform(axis: Vector3dc, angle: Double): Matrix4d {
        val pivot = Vector3d(axis).mul(-0.9 - 1.0 / 16.0)
        val positiveAxis = Vector3d(abs(axis.x()), abs(axis.y()), abs(axis.z()))
        return Matrix4d().translation(pivot).rotate(tilt).translate(Vector3d(pivot).negate())
            .rotate(Math.toRadians(angle), positiveAxis)
    }

    private fun radial(axis: Vector3dc): Vector3d =
        if (abs(axis.x()) < 0.5) Vector3d(2.0, 0.0, 0.0) else Vector3d(0.0, 2.0, 0.0)

    private fun prop(axis: Vector3dc, offset: Vector3dc) = PropData(
        Vector3i(), axis, 0.0, 18.0,
        listOf(Vector3i(offset.x().toInt(), offset.y().toInt(), offset.z().toInt())),
        false, true, true, emptyList()
    ).apply {
        currentBladePitch = Math.toRadians(4.0)
        bearingTiltQuat = tilt
        bearingAxisRot = tilt.transform(axis, Vector3d())
    }

    private fun solve(
        prop: PropData,
        flow: PropellerAerodynamics.Flow = PropellerAerodynamics.Flow(
            Matrix4d(), Vector3d(0.5, 0.5, 0.5), Vector3d(), Vector3d(), Vector3d()
        ) { 1.225 },
        angle: Double = 0.0
    ) = PropellerAerodynamics.compute(prop, flow, angle, 1.0, 1e12, 1e12)

    private fun assertVector(expected: Vector3dc, actual: Vector3dc) {
        assertTrue(expected.distance(actual) <= 1e-8 * maxOf(1.0, expected.length()), "Expected $expected, got $actual")
    }
}
