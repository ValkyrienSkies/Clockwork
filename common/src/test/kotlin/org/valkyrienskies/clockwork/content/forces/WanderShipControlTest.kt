package org.valkyrienskies.clockwork.content.forces

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.joml.Vector3d
import org.joml.Vector3i
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.world.PhysLevel

class WanderShipControlTest {
    @Test
    fun `lift acts at the mass-weighted center of the blocks`() {
        val controller = WanderShipControl()
        controller.wanderBlocks[Vector3i(100, 50, 200)] = 1000.0
        controller.wanderBlocks[Vector3i(103, 50, 200)] = 500.0

        val gravity = 9.0
        val level = mockk<PhysLevel> {
            every { dimension } returns "test"
            every { aerodynamicUtils.getAtmosphereForDimension("test") } returns Triple(0.0, 0.0, gravity)
        }
        val ship = mockk<PhysShip>(relaxed = true)

        controller.physTick(ship, level)

        val lift = 1500.0 * gravity * ClockworkConfig.SERVER.wanderOreForceMultiplier
        verify(exactly = 1) {
            ship.applyWorldForceToModelPos(Vector3d(0.0, lift, 0.0), Vector3d(101.5, 50.5, 200.5))
        }
    }
}
