package org.valkyrienskies.clockwork.content.forces

import org.joml.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData

class PropellerImpulseSolverTest {
    private val step = PropellerImpulseSolver.STEP
    private val com = Vector3d(.5, .5, 1.5)

    @Test
    fun `stopped rotors cannot add kinetic energy over a full physics tick`() {
        for (sails in listOf(false, true)) for (mass in listOf(1.0, 100.0, 10000.0))
            for (speed in listOf(10.0, 100.0, 1e6)) {
            val inertia = Matrix3d().scaling(mass * 2, mass * 3, mass * 4)
            val velocity = Vector3d(speed * .3, speed * -.2, speed)
            val omega = Vector3d(2.0, -3.0, 5.0)
            val flow = flow(velocity, omega)
            val props = listOf(rotor(0.0, sails), rotor(0.0, sails, Vector3i(3, -2, 1)))
            val before = energy(velocity, omega, mass, inertia)
            val loads = props.map { prepare(it, flow) }
            val result = PropellerImpulseSolver.solve(loads, mass, inertia)
            val nextV = Vector3d(velocity).add(Vector3d(result.first).mul(step / mass))
            val nextW = Vector3d(omega).add(Matrix3d(inertia).invert().transform(result.second, Vector3d()).mul(step))
            assertTrue(nextV.isFinite && nextW.isFinite)
            assertTrue(result.first.length() > 0.0)
            assertTrue(energy(nextV, nextW, mass, inertia) < before,
                "Stopped rotors added energy: sails=$sails, mass=$mass, speed=$speed")
        }
    }

    @Test
    fun `quadratic braking cannot launch a stopped ten ton ship backwards`() {
        val velocity = Vector3d(0.0, 0.0, 100.0)
        val load = prepare(rotor(0.0).apply { currentBladePitch = 0.0 }, flow(velocity))
        val rawNext = Vector3d(velocity).add(Vector3d(load.force).mul(step / 10000.0))
        assertTrue(rawNext.length() > velocity.length(), "Fixture must reproduce the old unstable impulse")
        val result = PropellerImpulseSolver.solve(listOf(load), 10000.0, Matrix3d().scale(40000.0))
        val next = Vector3d(velocity).add(Vector3d(result.first).mul(step / 10000.0))
        assertTrue(next.z in 0.0..100.0, "Implicit braking must slow this axial case without reversing it: $next")
    }

    @Test
    fun `powered sails remain bounded after sudden translation and spin at 60 Hz`() {
        for (mass in listOf(100.0, 5000.0, 10000.0, 50000.0)) for (rpm in listOf(-256.0, 64.0, 128.0, 256.0)) {
            val prop = rotor(rpm)
            val velocity = Vector3d()
            val omega = Vector3d()
            val inertia = Matrix3d().scale(mass * 4)
            repeat(600) { tick ->
                if (tick == 300) { velocity.z += 20.0; omega.z += 2.0 }
                val result = PropellerImpulseSolver.solve(listOf(prepare(prop, flow(velocity, omega))), mass, inertia)
                velocity.add(Vector3d(result.first).mul(step / mass))
                omega.add(Vector3d(result.second).mul(step / (mass * 4)))
                assertTrue(velocity.isFinite && omega.isFinite)
                assertTrue(velocity.length() < 200.0 && omega.length() < 100.0,
                    "Unbounded motion at tick $tick, mass=$mass, rpm=$rpm: v=$velocity, omega=$omega")
            }
        }
    }

    @Test
    fun `many rotors share one response and are independent of iteration order`() {
        val flow = flow(Vector3d(30.0, -20.0, 100.0), Vector3d(2.0, -1.0, 5.0))
        val loads = (0..7).map { prepare(rotor(0.0, position = Vector3i(it - 3, it % 2, 0)), flow) }
        val inertia = Matrix3d().scale(4000.0)
        val result = PropellerImpulseSolver.solve(loads, 1000.0, inertia)
        val reversed = PropellerImpulseSolver.solve(loads.reversed(), 1000.0, inertia)
        assertVector(result.first, reversed.first)
        assertVector(result.second, reversed.second)
        val nextV = Vector3d(flow.velocityWorld).add(Vector3d(result.first).mul(step / 1000.0))
        val nextW = Vector3d(flow.angularVelocityWorld).add(Vector3d(result.second).mul(step / 4000.0))
        assertTrue(energy(nextV, nextW, 1000.0, inertia) < energy(flow.velocityWorld, flow.angularVelocityWorld, 1000.0, inertia))
    }

    @Test
    fun `contra rotating sails still add thrust and cancel shaft reaction in the coupled solve`() {
        for (rpm in listOf(-256.0, -64.0, 64.0, 256.0)) {
            val normal = rotor(rpm)
            val reversed = rotor(-rpm).apply { inverted = true }
            val result = PropellerImpulseSolver.solve(listOf(prepare(normal, flow()), prepare(reversed, flow())),
                10000.0, Matrix3d().scale(40000.0))
            assertTrue(result.first.z() * rpm > 0.0)
            assertTrue(result.first.length() > 20000.0)
            assertTrue(result.second.length() < 1e-10 * result.first.length(),
                "Contra-rotating shaft moments should cancel to floating-point precision")
        }
    }

    @Test
    fun `implicit response rotates with wind torque arms copter tilt and anisotropic inertia`() {
        val rotation = Quaterniond().rotateXYZ(.7, -.4, .2)
        val matrix = Matrix3d().rotation(rotation)
        val inertia = Matrix3d().scaling(30000.0, 50000.0, 80000.0)
        val rotatedInertia = Matrix3d(matrix).mul(inertia).mul(matrix.transpose(Matrix3d()))
        val initial = flow(Vector3d(5.0, -10.0, 15.0), Vector3d(.4, -.5, .9), Vector3d(2.0, -3.0, 1.0))
        val rotated = initial.copy(modelToWorld = Matrix4d().rotate(rotation),
            velocityWorld = rotation.transform(initial.velocityWorld, Vector3d()),
            angularVelocityWorld = rotation.transform(initial.angularVelocityWorld, Vector3d()),
            windWorld = rotation.transform(initial.windWorld, Vector3d()))
        for (sails in listOf(false, true)) {
            fun prop() = rotor(256.0, sails, Vector3i(2, -1, 0)).apply {
                bearingTiltQuat = Quaterniond().rotateX(.2).rotateY(-.3)
            }
            val a = PropellerImpulseSolver.solve(listOf(prepare(prop(), initial)), 10000.0, inertia)
            val b = PropellerImpulseSolver.solve(listOf(prepare(prop(), rotated)), 10000.0, rotatedInertia)
            assertVector(rotation.transform(a.first, Vector3d()), b.first)
            assertVector(rotation.transform(a.second, Vector3d()), b.second)
        }
    }

    @Test
    fun `same world velocity added to ship and wind preserves the impulse`() {
        val boost = Vector3d(200.0, -50.0, 300.0)
        val base = flow(Vector3d(5.0, -3.0, 9.0), Vector3d(.2, -.3, .4), Vector3d(2.0, 1.0, -4.0))
        val boosted = base.copy(velocityWorld = Vector3d(base.velocityWorld).add(boost), windWorld = Vector3d(base.windWorld).add(boost))
        val a = PropellerImpulseSolver.solve(listOf(prepare(rotor(128.0), base)), 10000.0, Matrix3d().scale(40000.0))
        val b = PropellerImpulseSolver.solve(listOf(prepare(rotor(128.0), boosted)), 10000.0, Matrix3d().scale(40000.0))
        assertVector(a.first, b.first)
        assertVector(a.second, b.second)
    }

    @Test
    fun `weak responses retain raw loads and high RPM is not flattened by a force ceiling`() {
        var previousThrust = 0.0
        for (rpm in listOf(64.0, 128.0, 256.0)) {
            val load = prepare(rotor(rpm), flow())
            val heavy = PropellerImpulseSolver.solve(listOf(load), 1e14, Matrix3d().scale(4e14))
            assertVector(load.force, heavy.first)
            assertVector(load.torque, heavy.second)
            val ordinary = PropellerImpulseSolver.solve(listOf(load), 10000.0, Matrix3d().scale(40000.0))
            assertTrue(ordinary.first.z() > previousThrust)
            previousThrust = ordinary.first.z()
        }
        assertTrue(previousThrust > 200000.0)
    }

    @Test
    fun `configured force and torque limits also bound the integrated response`() {
        val flow = flow(Vector3d(10.0, -20.0, 100.0), Vector3d(1.0, 2.0, -3.0))
        val load = prepare(rotor(256.0, position = Vector3i(4, -2, 0)), flow)
        val result = PropellerImpulseSolver.solve(listOf(load), 1000.0, Matrix3d().scale(4000.0), 200.0, 50.0)
        assertTrue(result.first.length() <= 200.0 + 1e-8)
        assertTrue(result.second.length() <= 50.0 + 1e-8)
    }

    @Test
    fun `invalid mass properties never send a nonfinite force to physics`() {
        val load = prepare(rotor(256.0), flow())
        for (mass in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val result = PropellerImpulseSolver.solve(listOf(load), mass, Matrix3d())
            assertVector(Vector3d(), result.first); assertVector(Vector3d(), result.second)
        }
        for (inertia in listOf(Matrix3d().zero(), Matrix3d().scaling(-1.0, 1.0, 1.0), Matrix3d().scale(Double.NaN))) {
            val result = PropellerImpulseSolver.solve(listOf(load), 1000.0, inertia)
            assertVector(Vector3d(), result.first); assertVector(Vector3d(), result.second)
        }
    }

    private fun rotor(rpm: Double, sails: Boolean = true, position: Vector3i = Vector3i()) = PropData(
        position, Vector3d(0.0, 0.0, 1.0), 0.0, rpm * .3,
        (1..3).flatMap { r -> listOf(Vector3i(r, 0, 0), Vector3i(-r, 0, 0), Vector3i(0, r, 0), Vector3i(0, -r, 0)) },
        false, true, sails, if (sails) emptyList() else List(8) { BladeData(true, -12.0, 3.5) }
    )

    private fun flow(velocity: Vector3dc = Vector3d(), omega: Vector3dc = Vector3d(), wind: Vector3dc = Vector3d()) =
        PropellerAerodynamics.Flow(Matrix4d(), com, velocity, omega, wind) { 1.225 }

    private fun prepare(prop: PropData, flow: PropellerAerodynamics.Flow) =
        PropellerAerodynamics.prepare(prop, flow, 37.0, if (prop.brass) 960.0 else 240.0, 0.0, 0.0)

    private fun energy(v: Vector3dc, w: Vector3dc, mass: Double, inertia: Matrix3dc) =
        .5 * (mass * v.lengthSquared() + w.dot(inertia.transform(w, Vector3d())))

    private fun assertVector(expected: Vector3dc, actual: Vector3dc) {
        assertTrue(expected.distance(actual) <= 1e-8 * maxOf(1.0, expected.length()), "Expected $expected, got $actual")
    }
}
