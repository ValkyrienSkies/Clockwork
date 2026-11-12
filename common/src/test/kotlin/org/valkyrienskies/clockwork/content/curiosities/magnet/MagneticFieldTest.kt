package org.valkyrienskies.clockwork.content.curiosities.magnet

import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MagneticFieldTest {
    private fun magnet(x: Double, sign: Double = 1.0) =
        MagneticField.poles(Vector3d(x, 0.0, 0.0), Vector3d(sign, 0.0, 0.0), 1.0)

    private fun force(a: List<MagneticField.Pole>, b: List<MagneticField.Pole>) = Vector3d().also { result ->
        for (pa in a) for (pb in b) result.add(MagneticField.pairForce(pa, pb))
    }

    @Test
    fun `opposite faces attract and matching faces repel with balanced reactions`() {
        val a = magnet(0.0)
        val b = magnet(2.0)
        assertTrue(force(a, b).x > 0.0)
        assertTrue(force(a, magnet(1.0)).x > 0.0)
        assertTrue(force(a, magnet(2.0, -1.0)).x < 0.0)
        assertTrue(force(a, b).add(force(b, a)).length() < 1e-8)
        val torque = Vector3d()
        for (pa in a) for (pb in b) {
            val f = MagneticField.pairForce(pa, pb)
            torque.add(Vector3d(pa.position).cross(f)).sub(Vector3d(pb.position).cross(f))
        }
        assertTrue(torque.length() < 1e-8)
    }

    @Test
    fun `extended poles generate alignment torque`() {
        val a = magnet(0.0)
        val b = MagneticField.poles(Vector3d(2.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), 1.0)
        val torque = Vector3d()
        for (pa in a) for (pb in b) {
            torque.add(Vector3d(pb.position).sub(2.0, 0.0, 0.0).cross(MagneticField.pairForce(pb, pa)))
        }
        assertTrue(torque.z < -1.0)
    }

    @Test
    fun `iron seeks either pole and responds to the combined field`() {
        val point = Vector3d(2.0, 0.2, 0.1)
        fun attraction(poles: List<MagneticField.Pole>): Vector3d {
            val field = MagneticField.field(poles, point)
            return Vector3d().also { result -> poles.forEach { result.add(MagneticField.ironForce(it, point, field, 1.0)) } }
        }
        val a = magnet(0.0)
        val reversed = magnet(0.0, -1.0)
        assertTrue(attraction(a).x < 0.0)
        assertTrue(attraction(a).distance(attraction(reversed)) < 1e-8)
        assertTrue(attraction(a + a).length() > attraction(a).length())
        assertTrue(attraction(a + reversed).length() < 1e-8)
    }

    @Test
    fun `iron gradient matches energy and conserves angular momentum across several sources`() {
        val point = Vector3d(2.0, 0.3, 0.4)
        val poles = magnet(0.0) + MagneticField.poles(Vector3d(1.0, 3.0, 0.0), Vector3d(0.0, 1.0, 0.0), 1.4)
        val field = MagneticField.field(poles, point)
        val total = Vector3d()
        val torque = Vector3d()
        for (pole in poles) {
            val f = MagneticField.ironForce(pole, point, field, 1.0)
            total.add(f)
            torque.add(Vector3d(point).cross(f)).sub(Vector3d(pole.position).cross(f))
        }
        assertTrue(torque.length() < 1e-7)
        fun energy(p: Vector3d) = -MagneticField.FORCE * 4.0 * kotlin.math.sqrt(1.0 + MagneticField.field(poles, p).lengthSquared() / 4.0)
        val h = 1e-4
        val numericalX = -(energy(Vector3d(point).add(h, 0.0, 0.0)) - energy(Vector3d(point).sub(h, 0.0, 0.0))) / (2.0 * h)
        assertEquals(numericalX, total.x, 0.01)
    }

    @Test
    fun `contact and range cutoff remain finite and continuous`() {
        val pole = MagneticField.Pole(Vector3d(), 1.0)
        val coincident = MagneticField.pairForce(pole, pole)
        assertTrue(coincident.isFinite && coincident.lengthSquared() == 0.0)
        assertTrue(MagneticField.ironForce(pole, Vector3d(), Vector3d(1.0), 1.0).isFinite)
        val beyond = MagneticField.Pole(Vector3d(MagneticField.RANGE, 0.0, 0.0), 1.0)
        assertEquals(0.0, MagneticField.pairForce(pole, beyond).length())
        val inside = MagneticField.Pole(Vector3d(MagneticField.RANGE - 1e-4, 0.0, 0.0), 1.0)
        assertTrue(MagneticField.pairForce(pole, inside).length() < 1e-8)
    }
}
