package org.valkyrienskies.clockwork.content.forces

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.joml.Vector3ic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.internal.ships.VsiPhysShip
import kotlin.math.PI

/** Calls the production force functions; only the ship, atmosphere and wind are mocked. */
class PropellerForceRegressionTest {
    private val controller = PropellerController()
    private val rho = 1.225
    private val level = mockk<PhysLevel> {
        every { aerodynamicUtils.getAirDensityForY(any(), any()) } returns rho
        every { aerodynamicUtils.getAirTemperatureForY(any(), any()) } returns 288.15
    }
    private var savedMultiplier = 0.0
    private var savedMaxForce = 0.0
    private var savedMaxTorque = 0.0

    @BeforeEach
    fun configureForces() {
        savedMultiplier = ClockworkConfig.SERVER.forceMulPerSailInPropeller
        savedMaxForce = ClockworkConfig.SERVER.propellerMaxForce
        savedMaxTorque = ClockworkConfig.SERVER.propellerMaxTorque
        ClockworkConfig.SERVER.forceMulPerSailInPropeller = 12.0
        // Check the equations below the limits; coupled force/moment limiting is tested separately.
        ClockworkConfig.SERVER.propellerMaxForce = 1e12
        ClockworkConfig.SERVER.propellerMaxTorque = 1e12
    }

    @AfterEach
    fun restoreConfig() {
        ClockworkConfig.SERVER.forceMulPerSailInPropeller = savedMultiplier
        ClockworkConfig.SERVER.propellerMaxForce = savedMaxForce
        ClockworkConfig.SERVER.propellerMaxTorque = savedMaxTorque
    }

    @Test
    fun `bearing speed conversion preserves sign and uses seconds`() {
        assertEquals(2.0 * PI, controller.bearingSpeedToRadiansPerSecond(18.0), 1e-12)
        assertEquals(-256.0 * 2.0 * PI / 60.0, controller.bearingSpeedToRadiansPerSecond(-76.8), 1e-12)
        assertEquals(0.0, controller.bearingSpeedToRadiansPerSecond(0.0), 0.0)
    }

    @Test
    fun `default tuning rewards higher RPM beyond the old force ceiling`() {
        val defaults = ClockworkConfig.Server()
        ClockworkConfig.SERVER.forceMulPerSailInPropeller = defaults.forceMulPerSailInPropeller
        ClockworkConfig.SERVER.propellerMaxForce = defaults.propellerMaxForce
        ClockworkConfig.SERVER.propellerMaxTorque = defaults.propellerMaxTorque
        for (sails in listOf(false, true)) {
            fun at(rpm: Double) = forces(PropData(
                Vector3i(), Vector3d(0.0, 0.0, 1.0), 0.0, rpm * 0.3,
                (1..2).flatMap { r -> listOf(Vector3i(r, 0, 0), Vector3i(-r, 0, 0), Vector3i(0, r, 0), Vector3i(0, -r, 0)) },
                false, true, sails, List(4) { BladeData(false, -12.0, 4.0) }
            ).apply { currentBladePitch = Math.toRadians(4.0) }, ship())
            val low = at(64.0)
            val medium = at(128.0)
            val high = at(256.0)
            assertTrue(low.first.z() > 20000.0, "A modest rotor should produce useful low-RPM thrust")
            assertTrue(high.first.z() > 200000.0, "High RPM must not flatten at the old global cap")
            assertVectorEquals(Vector3d(low.first).mul(4.0), medium.first)
            assertVectorEquals(Vector3d(medium.first).mul(4.0), high.first)
            assertVectorEquals(Vector3d(low.second).mul(16.0), high.second)
        }
    }

    @Test
    fun `sail static thrust uses one revolution per second at sixty RPM`() {
        val prop = propeller(sails = true, rpm = 60.0)
        val speedBefore = prop.bearingSpeed
        val force = forces(prop, ship()).first
        // Unit-radius sail: U = 2*pi m/s, q = rho*U^2/2, CL = 2*pi*4 degrees.
        val liftCoefficient = 2.0 * PI * prop.currentBladePitch
        val expected = 0.5 * rho * (2.0 * PI) * (2.0 * PI) * liftCoefficient * 12.0
        assertEquals(expected, force.z(), 1e-8)
        assertEquals(speedBefore, prop.bearingSpeed, 0.0, "Animation speed must remain degrees per tick")
    }

    @Test
    fun `blade static thrust uses SI tip speed`() {
        val prop = propeller(sails = false, rpm = 60.0, pitch = -4.0)
        val force = forces(prop, ship()).first
        // Integrating q(r) along a unit-radius, constant-chord blade gives one third of q(tip).
        val tipPressure = 0.5 * rho * 4.0 * PI * PI
        val liftCoefficient = 2.0 * PI * Math.toRadians(4.0)
        assertEquals(tipPressure / 3.0 * 0.25 * liftCoefficient * 12.0, force.z(), 1e-8)
    }

    @Test
    fun `both propeller models reverse static thrust with rotation`() {
        for (sails in listOf(true, false)) {
            for (rpm in listOf(20.0, 60.0, 256.0)) {
                val forward = forces(propeller(sails, rpm), ship()).first
                val reverse = forces(propeller(sails, -rpm), ship()).first
                assertTrue(forward.z() > 0.0)
                assertVectorEquals(Vector3d(forward).negate(), reverse)
            }
        }
    }

    @Test
    fun `blade pitch reverses thrust in either rotation direction`() {
        for (rpm in listOf(-60.0, 60.0)) {
            val positivePitch = forces(propeller(false, rpm, pitch = -15.0), ship()).first
            val negativePitch = forces(propeller(false, rpm, pitch = 15.0), ship()).first
            assertTrue(positivePitch.length() > 0.0)
            assertEquals(-positivePitch.z(), negativePitch.z(), 1e-8)
            // Profile drag opposes the same rotation for either pitch.
            assertEquals(positivePitch.x(), negativePitch.x(), 1e-8)
            assertEquals(positivePitch.y(), negativePitch.y(), 1e-8)
        }
    }

    @Test
    fun `zero pitch resists axial motion for both rotation directions`() {
        for (speed in listOf(-100.0, -20.0, 20.0, 100.0)) {
            for (rpm in listOf(20.0, 256.0)) {
                val movingShip = ship(velocity = Vector3d(0.0, 0.0, speed))
                val forward = forces(propeller(false, rpm, pitch = 0.0), movingShip).first
                val reverse = forces(propeller(false, -rpm, pitch = 0.0), movingShip).first
                assertTrue(forward.z() * speed < 0.0, "Zero-pitch force must oppose motion")
                assertEquals(forward.z(), reverse.z(), 1e-8)
            }
        }
    }

    @Test
    fun `co-moving air and ship give the same forces as still air`() {
        val drift = Vector3d(4.0, -3.0, 10.0)
        for (sails in listOf(true, false)) {
            for (rpm in listOf(-60.0, 60.0)) {
                val still = forces(propeller(sails, rpm), ship())
                val drifting = forces(propeller(sails, rpm), ship(velocity = drift, wind = drift))
                assertVectorEquals(still.first, drifting.first)
                assertVectorEquals(still.second, drifting.second)
            }
        }
        assertVectorEquals(Vector3d(4.0, -3.0, 10.0), drift)
    }

    @Test
    fun `adding the same velocity to ship and wind preserves forces in nonzero airflow`() {
        val velocity = Vector3d(1.0, -2.0, 3.0)
        val wind = Vector3d(-4.0, 2.0, -1.0)
        val boost = Vector3d(7.0, 9.0, 11.0)
        val omega = Vector3d(0.2, -0.5, 0.3)
        for (sails in listOf(true, false)) {
            for (rpm in listOf(-60.0, 60.0)) {
                val base = forces(propeller(sails, rpm), ship(velocity = velocity, wind = wind, omega = omega))
                val boosted = forces(propeller(sails, rpm), ship(
                    velocity = Vector3d(velocity).add(boost), wind = Vector3d(wind).add(boost), omega = omega
                ))
                assertVectorEquals(base.first, boosted.first)
                assertVectorEquals(base.second, boosted.second)
            }
        }
    }

    @Test
    fun `blade force rotates with the entire world frame`() {
        val rotations = listOf(Quaterniond().rotateY(PI / 2.0), Quaterniond().rotateXYZ(0.4, -0.7, 1.1))
        val velocity = Vector3d(1.0, -2.0, 3.0)
        val wind = Vector3d(-0.5, 1.0, -2.0)
        val omega = Vector3d(0.3, -0.8, 0.2)
        for (scale in listOf(1.0, 2.0)) {
            for (rotation in rotations) {
                val base = forces(propeller(false, position = Vector3i(3, -2, 1)), ship(
                    velocity = velocity, wind = wind, omega = omega, scale = scale
                )).first
                val rotated = forces(propeller(false, position = Vector3i(3, -2, 1)), ship(
                    velocity = rotation.transform(velocity, Vector3d()),
                    wind = rotation.transform(wind, Vector3d()),
                    omega = rotation.transform(omega, Vector3d()), rotation = rotation, scale = scale
                )).first
                assertVectorEquals(rotation.transform(base, Vector3d()), rotated)
            }
        }
    }

    @Test
    fun `uniform scaling applies to area speed and moment arm`() {
        for (sails in listOf(true, false)) {
            val base = forces(propeller(sails), ship())
            val scaled = forces(propeller(sails), ship(scale = 2.0))
            assertVectorEquals(Vector3d(base.first).mul(16.0), scaled.first)
            assertVectorEquals(Vector3d(base.second).mul(32.0), scaled.second)
        }
    }

    @Test
    fun `blade force is independent of shipyard origin`() {
        val omega = Vector3d(0.0, 0.5, 0.0)
        val base = forces(propeller(false, position = Vector3i(2, 0, 0)), ship(omega = omega)).first
        val translated = forces(propeller(false, position = Vector3i(10002, 20000, 30000)), ship(
            omega = omega, com = Vector3d(10000.5, 20000.5, 30000.5)
        )).first
        assertVectorEquals(base, translated)
    }

    @Test
    fun `sail torque is the moment of the fully scaled force in both directions`() {
        val rotation = Quaterniond().rotateXYZ(0.4, -0.7, 1.1)
        val rotorAngle = 37.0
        val leverArm = rotation.transform(Vector3d(1.0, 0.0, 0.0).rotateZ(Math.toRadians(rotorAngle)).add(0.0, 0.0, 1.0))
        for (multiplier in listOf(0.0, 1.0, 12.0)) {
            ClockworkConfig.SERVER.forceMulPerSailInPropeller = multiplier
            for (rpm in listOf(-60.0, 60.0)) {
                val prop = propeller(true, rpm).apply { bearingAngle = rotorAngle }
                val (force, torque) = forces(prop, ship(rotation = rotation))
                assertVectorEquals(leverArm.cross(force, Vector3d()), torque)
                if (multiplier == 0.0) {
                    assertVectorEquals(Vector3d(), force)
                    assertVectorEquals(Vector3d(), torque)
                } else {
                    assertTrue(force.length() > 0.0)
                    assertTrue(torque.length() > 0.0)
                }
            }
        }
    }

    @Test
    fun `stopped rotors produce finite zero force and torque`() {
        for (sails in listOf(true, false)) {
            val (force, torque) = forces(propeller(sails, rpm = 0.0), ship())
            assertTrue(force.isFinite && torque.isFinite)
            assertVectorEquals(Vector3d(), force)
            assertVectorEquals(Vector3d(), torque)
        }
    }

    @Test
    fun `physics tick applies each complete force and moment at the center of mass`() {
        for (sails in listOf(true, false)) {
            val physicsShip = ship()
            val expected = forces(propeller(sails), physicsShip)
            val tickingController = PropellerController()
            tickingController.appliers[0] = propeller(sails)
            tickingController.physTick(physicsShip, level)
            verify(exactly = 1) { physicsShip.applyWorldForceToBodyPos(
                match { it.distance(expected.first) < 1e-8 }, match { it.lengthSquared() == 0.0 }
            ) }
            verify(exactly = 1) { physicsShip.applyWorldTorque(match { it.distance(expected.second) < 1e-8 }) }
            verify(exactly = 0) { physicsShip.applyWorldForceToModelPos(any(), any()) }
        }
    }

    private fun propeller(
        sails: Boolean, rpm: Double = 60.0, pitch: Double = -15.0, position: Vector3ic = Vector3i()
    ) = PropData(
        position, Vector3d(0.0, 0.0, 1.0), 0.0, rpm * 0.3,
        listOf(Vector3i(1, 0, 0)), false, true, sails, listOf(BladeData(false, pitch, 1.0))
    ).apply {
        // Start collective pitch at its static target to isolate each force invariant.
        currentBladePitch = Math.toRadians(4.0).toFloat().toDouble()
    }

    private fun ship(
        velocity: Vector3dc = Vector3d(), wind: Vector3dc = Vector3d(), omega: Vector3dc = Vector3d(),
        rotation: Quaterniond = Quaterniond(), scale: Double = 1.0,
        com: Vector3dc = Vector3d(0.5, 0.5, 0.5)
    ): VsiPhysShip {
        val worldCom = Vector3d(20.0, 64.0, -10.0)
        val matrix = Matrix4d().translation(worldCom).rotate(rotation).scale(scale)
            .translate(Vector3d(com).negate())
        val transform = mockk<ShipTransform> {
            every { shipToWorld } returns matrix
            every { positionInWorld } returns worldCom
            every { positionInShip } returns com
        }
        return mockk(relaxed = true) {
            every { this@mockk.transform } returns transform
            every { this@mockk.velocity } returns velocity
            every { angularVelocity } returns omega
            every { this@mockk.omega } returns omega
            every { centerOfMass } returns com
            every { dragController!!.getWindVector() } returns wind
        }
    }

    private fun forces(prop: PropData, ship: VsiPhysShip): Pair<Vector3dc, Vector3dc> =
        if (prop.brass) controller.computeForce(ship.transform, prop, ship.velocity, ship.omega, ship, level)
        else controller.computeBladeForce(ship, prop, level)

    private fun assertVectorEquals(expected: Vector3dc, actual: Vector3dc) {
        val tolerance = 1e-8 * maxOf(1.0, expected.length())
        assertTrue(expected.distance(actual) <= tolerance, "Expected $expected, got $actual")
    }
}
