package org.valkyrienskies.clockwork.content.forces

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronForceInducerData
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroShipControl
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel

class GravitronGyroTest {
    @Test
    fun gyroStopsWhileHeldAndResumesAfterRelease() {
        val gyro = GyroShipControl().apply {
            gyros = 1
            speed = 128f
        }
        val ship = mockk<LoadedServerShip> {
            every { getAttachment(GyroShipControl::class.java) } returns gyro
        }
        val physics = mockk<PhysShip>(relaxed = true) {
            every { transform.shipToWorldRotation } returns Quaterniond().rotateZ(0.3)
            every { angularVelocity } returns Vector3d()
            every { momentOfInertia } returns Matrix3d()
        }
        val level = mockk<PhysLevel>()
        val gravitron = GravitronController()

        gravitron.setData(GravitronForceInducerData(Vector3d(), Quaterniond(), Vector3d()), ship)
        gyro.physTick(physics, level)
        verify(exactly = 0) { physics.applyWorldTorque(any()) }

        gravitron.setData(null, ship)
        gyro.physTick(physics, level)
        verify(exactly = 1) { physics.applyWorldTorque(match { it.isFinite && it.lengthSquared() > 0.0 }) }
    }
}
