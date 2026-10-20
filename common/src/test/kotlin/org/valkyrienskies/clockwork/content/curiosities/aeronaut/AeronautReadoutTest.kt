package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AeronautReadoutTest {
    @Test
    fun `unequal engines weight their actual application points`() {
        val forces = AeronautForceAccumulator()
        forces.add(Vector3d(-10.0, 0.0, 0.0), Vector3d(0.0, 0.0, 100.0))
        forces.add(Vector3d(10.0, 0.0, 0.0), Vector3d(0.0, 0.0, 300.0))
        val center = forces.center()!!
        assertEquals(5.0, center.position.x(), 1e-9)
        assertEquals(400.0, center.force.z(), 1e-9)
        assertEquals(400.0, center.weight, 1e-9)
    }

    @Test
    fun `opposing engines keep a finite balance point even with no net thrust`() {
        val pair = AeronautForceAccumulator()
        pair.add(Vector3d(-4.0, 0.0, 0.0), Vector3d(0.0, 0.0, 100.0))
        pair.add(Vector3d(4.0, 0.0, 0.0), Vector3d(0.0, 0.0, -100.0))
        assertEquals(Vector3d(), pair.center()!!.force)
        val ship = AeronautForceAccumulator()
        ship.add(pair.center())
        ship.add(Vector3d(9.0, 0.0, 0.0), Vector3d(0.0, 0.0, 100.0))
        assertEquals(3.0, ship.center()!!.position.x(), 1e-9)
    }

    @Test
    fun `missing stopped and nonfinite contributors do not create phantom markers`() {
        val forces = AeronautForceAccumulator()
        forces.add(Vector3d(), Vector3d())
        forces.add(Vector3d(Double.NaN, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0))
        forces.add(Vector3d(), Vector3d(Double.POSITIVE_INFINITY, 0.0, 0.0))
        assertNull(forces.center())
    }

    @Test
    fun `wing lift disappears at rest and stays perpendicular to airflow`() {
        val normal = Vector3d(0.0, 1.0, 0.0)
        assertEquals(Vector3d(), AeronautForceReadout.wingLift(normal, Vector3d(), 1.225, 150.0, .17))
        val flow = Vector3d(0.0, -2.0, 10.0)
        val lift = AeronautForceReadout.wingLift(normal, flow, 1.225, 150.0, .17)
        assertTrue(lift.y > 0)
        assertEquals(0.0, flow.dot(lift), 1e-8)
        assertEquals(Vector3d(), AeronautForceReadout.wingLift(normal, flow, 0.0, 150.0, .17))
    }

    @Test
    fun `physics telemetry is opt in rate limited and replaced on the next sample`() {
        AeronautForceTelemetry.clear()
        try {
            assertNull(AeronautForceTelemetry.begin(10L, "engine"))
            AeronautForceTelemetry.watch(10L)
            val forces = AeronautForceTelemetry.begin(10L, "engine")!!
            forces.add(Vector3d(2.0, 3.0, 4.0), Vector3d(0.0, 10.0, 0.0))
            AeronautForceTelemetry.publish(10L, "engine", forces)
            assertNull(AeronautForceTelemetry.begin(10L, "engine"))
            assertEquals(2.0, AeronautForceTelemetry.thrust(10L)!!.position.x())
            AeronautForceTelemetry.publish(10L, "engine", AeronautForceAccumulator())
            assertNull(AeronautForceTelemetry.thrust(10L))
            assertNull(AeronautForceTelemetry.thrust(11L))
        } finally { AeronautForceTelemetry.clear() }
    }
}
