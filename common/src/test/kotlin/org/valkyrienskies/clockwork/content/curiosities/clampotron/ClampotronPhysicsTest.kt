package org.valkyrienskies.clockwork.content.curiosities.clampotron

import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronManipulation

class ClampotronPhysicsTest {
    private class Body(val mass: Double, val inertia: Double, val position: Vector3d, val local: Vector3d) {
        val rotation = Quaterniond()
        val velocity = Vector3d()
        val angular = Vector3d()
        fun offset() = rotation.transform(local, Vector3d())
        fun point() = Vector3d(position).add(offset())
        fun physics(point: Vector3d, responseMass: Double = mass) = ClampotronPhysics.Body(
            1.0 / responseMass, Matrix3d().scaling(mass / (inertia * responseMass)), Vector3d(point).sub(position))
        fun integrate(force: Vector3d, torque: Vector3d, step: Double) {
            velocity.fma(step / mass, force)
            angular.fma(step / inertia, torque)
            position.fma(step, velocity)
            if (angular.lengthSquared() > 1e-16) {
                val axis = Vector3d(angular).normalize()
                rotation.premul(Quaterniond().rotationAxis(angular.length() * step, axis.x, axis.y, axis.z)).normalize()
            }
        }
    }

    @Test fun `heavy cargo on a light carrier settles without runaway at long reach`() {
        for (step in listOf(1.0 / 60.0, 1.0 / 20.0)) {
            for (reach in listOf(3.0, 32.0)) simulate(step, reach, 100000.0)
        }
    }

    @Test fun `joint load exceeding the grabbed body mass remains bounded`() {
        simulate(1.0 / 60.0, 6.0, 1000.0)
        simulate(1.0 / 20.0, 6.0, 1000.0)
    }

    private fun simulate(step: Double, reach: Double, responseMass: Double) {
        val cargo = Body(100000.0, 80000.0, Vector3d(reach, 0.5, 0.0), Vector3d(0.0, -0.5, 0.0))
        val carrier = Body(100.0, 80.0, Vector3d(0.0, -0.5, 0.0), Vector3d(reach, 0.0, 0.0))
        val initialError = cargo.point().distance(carrier.point())
        repeat((12.0 / step).toInt()) {
            val anchor = cargo.point()
            val a = cargo.physics(anchor, responseMass)
            val b = carrier.physics(anchor)
            val relative = GravitronManipulation.pointVelocity(cargo.velocity, cargo.angular, a.offset)
                .sub(GravitronManipulation.pointVelocity(carrier.velocity, carrier.angular, b.offset))
            val rotationError = Quaterniond(carrier.rotation).mul(Quaterniond(cargo.rotation).invert()).normalize()
            val angularError = Vector3d(rotationError.x, rotationError.y, rotationError.z)
                .mul(if (rotationError.w < 0) -2.0 else 2.0)
            val wrench = ClampotronPhysics.solve(a, b, carrier.point().sub(anchor), relative,
                angularError, Vector3d(cargo.angular).sub(carrier.angular),
                1.0 / (1.0 / cargo.mass + 1.0 / carrier.mass), 0.0, step)
            val cargoTorque = Vector3d(a.offset).cross(wrench.force).add(wrench.torque)
            val carrierTorque = Vector3d(b.offset).cross(wrench.force).add(wrench.torque).negate()
            val totalTorque = Vector3d(cargo.position).cross(wrench.force).add(cargoTorque)
                .sub(Vector3d(carrier.position).cross(wrench.force)).add(carrierTorque)
            assertTrue(totalTorque.length() < 1e-6, "Grip injected angular momentum")
            cargo.integrate(wrench.force, cargoTorque, step)
            carrier.integrate(Vector3d(wrench.force).negate(), carrierTorque, step)
            assertTrue(cargo.point().distance(carrier.point()) < initialError * 2, "Grip diverged at reach $reach")
            assertTrue(cargo.angular.length() < 10 && carrier.angular.length() < 10, "Grip caused runaway spin")
        }
        assertTrue(cargo.point().distance(carrier.point()) < 0.1, "Grip failed to settle at reach $reach")
        assertTrue(Vector3d(cargo.velocity).mul(cargo.mass).fma(carrier.mass, carrier.velocity).length() < 1e-6)
    }

    @Test fun `free falling pair needs no gravity correction while fixed mount supports weight`() {
        val body = ClampotronPhysics.Body(0.001, Matrix3d().scaling(0.001), Vector3d())
        val fixed = ClampotronPhysics.Body(0.0, Matrix3d().zero(), Vector3d())
        val zero = Vector3d()
        val falling = ClampotronPhysics.solve(body, body, zero, zero, zero, zero, 500.0, 0.0, 1.0 / 60)
        assertEquals(0.0, falling.force.length(), 1e-8)
        val supported = ClampotronPhysics.solve(body, fixed, zero, zero, zero, zero, 1000.0, 9.81, 1.0 / 60)
        assertEquals(9810.0, supported.force.y, 1e-8)
        val offset = body.copy(offset = Vector3d(2.0, 0.0, 0.0))
        val offCenter = ClampotronPhysics.solve(offset, fixed, zero, zero, zero, zero, 1000.0, 9.81, 1.0 / 60)
        assertEquals(9810.0, offCenter.force.y, 1e-8)
        assertEquals(0.0, ClampotronPhysics.response(offset, offCenter.force, offCenter.torque).torque.length(), 1e-8)
    }
}
