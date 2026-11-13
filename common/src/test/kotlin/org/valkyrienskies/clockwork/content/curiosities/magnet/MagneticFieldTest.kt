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
    fun `long range pull improves without increasing contact force`() {
        val a = magnet(0.0)
        assertTrue(force(a, magnet(1.0)).x in 90000.0..125000.0)
        assertTrue(force(a, magnet(2.0)).x in 24000.0..36000.0)
        assertTrue(force(a, magnet(4.0)).x > 3400.0)
        assertTrue(force(a, magnet(8.0)).x > 400.0)
        assertTrue(force(a, magnet(12.0)).x > 150.0)
        var previous = Double.POSITIVE_INFINITY
        for (i in 100..150) assertTrue(force(a, magnet(i / 100.0)).x in 0.0..130000.0)
        for (i in 15..160) {
            val pull = force(a, magnet(i / 10.0)).x
            assertTrue(pull >= 0.0 && pull < previous, "Pull reversed or increased at ${i / 10.0} blocks")
            previous = pull
        }
    }

    @Test
    fun `cached iron samples preserve the field and every source reaction`() {
        val poles = magnet(0.0) + magnet(4.0, -1.0) + magnet(30.0)
        val sample = MagneticField.Sample()
        val output = Vector3d()
        for (point in listOf(Vector3d(2.0, .2, .1), Vector3d(15.9, 0.0, 0.0), Vector3d(50.0))) {
            MagneticField.sample(poles, point, sample)
            val field = MagneticField.field(poles, point)
            assertTrue(sample.field.distance(field) < 1e-10)
            for ((i, pole) in poles.withIndex()) {
                val expected = MagneticField.ironForce(pole, point, field, .125)
                MagneticField.ironForce(sample, i, .125, output)
                assertTrue(output.distance(expected) < 1e-8)
            }
        }
        val single = poles.take(1)
        MagneticField.sample(single, Vector3d(), sample)
        assertTrue(sample.field.distance(MagneticField.field(single, Vector3d())) < 1e-10)
    }

    @Test
    fun `distant pole reduction preserves force direction and alignment torque`() {
        val a = magnet(0.0)
        val coarseA = MagneticField.coarsePoles(a)
        for (position in listOf(Vector3d(8.0, 0.0, 0.0), Vector3d(0.0, 8.0, 0.0), Vector3d(6.0, 5.0, 1.0))) {
            for (normal in listOf(Vector3d(1.0, 0.0, 0.0), Vector3d(0.0, 1.0, 0.0), Vector3d(1.0, 1.0, 1.0).normalize())) {
                val b = MagneticField.poles(position, normal, 1.0)
                val coarseB = MagneticField.coarsePoles(b)
                val exact = force(a, b)
                val reduced = force(coarseA, coarseB)
                assertTrue(exact.distance(reduced) < exact.length() * .05)
                fun torque(first: List<MagneticField.Pole>, second: List<MagneticField.Pole>) = Vector3d().also { result ->
                    for (pa in first) for (pb in second) {
                        result.add(Vector3d(pa.position).cross(MagneticField.pairForce(pa, pb)))
                    }
                }
                val exactTorque = torque(a, b)
                assertTrue(exactTorque.distance(torque(coarseA, coarseB)) < maxOf(exactTorque.length(), exact.length()) * .05)
                assertTrue(reduced.add(force(coarseB, coarseA)).length() < 1e-8)
            }
        }
    }

    @Test
    fun `iron cores relay and reshape the field along an iron chain`() {
        val poles = magnet(0.0)
        val centers = (1..5).map { Vector3d(it.toDouble(), 0.0, 0.0) }
        val solver = MagneticField.CoreSolver()
        val moments = solver.solve(poles, centers)
        assertTrue(moments.all { it.x > 0.0 && kotlin.math.abs(it.y) < 1e-9 && kotlin.math.abs(it.z) < 1e-9 })
        val isolated = MagneticField.CoreSolver().solve(poles, listOf(centers.last())).single()
        assertTrue(moments.last().length() > isolated.length(), "The chain must relay induction to its far end")
        val reshaped = poles + centers.indices.flatMap { MagneticField.corePoles(centers[it], moments[it]) }
        val probe = Vector3d(5.8, .2, 0.0)
        assertTrue(MagneticField.field(reshaped, probe).length() > MagneticField.field(poles, probe).length() * 1.5)
        val offAxis = Vector3d(4.0, 1.0, 0.0)
        assertTrue(MagneticField.field(reshaped, offAxis).normalize().distance(MagneticField.field(poles, offAxis).normalize()) > .1)
        val reversed = MagneticField.CoreSolver().solve(magnet(0.0, -1.0), centers)
        for (i in centers.indices) assertTrue(Vector3d(reversed[i]).add(moments[i]).length() < 1e-8)
    }

    @Test
    fun `core saturation stays bounded and turning the magnet off clears cached induction`() {
        val centers = (0 until 64).map { Vector3d((it % 4).toDouble(), (it / 4 % 4).toDouble(), (it / 16).toDouble()) }
        val solver = MagneticField.CoreSolver()
        val poles = MagneticField.poles(Vector3d(-1.0, 0.0, 0.0), Vector3d(1.0, 0.0, 0.0), 1000.0)
        val moments = solver.solve(poles, centers)
        assertTrue(moments.all { it.isFinite && it.length() <= 1.5 })
        val saved = moments.map(::Vector3d)
        assertTrue(solver.solve(poles, centers) === moments)
        assertTrue(solver.solve(emptyList(), centers).all { it.lengthSquared() == 0.0 })
        val restored = solver.solve(poles, centers)
        for (i in centers.indices) assertTrue(restored[i].distance(saved[i]) < 1e-10)
        assertTrue(solver.solve(poles, emptyList()).isEmpty())
        val coincident = solver.solve(poles, List(8) { Vector3d() })
        assertTrue(coincident.all { it.isFinite && it.length() <= 1.5 })
        val moving = listOf(Vector3d(1.0, 0.0, 0.0), Vector3d(6.2, 0.0, 0.0))
        for (x in listOf(6.2, 5.4, 5.1, 4.9, 4.7, 3.0, 8.0)) {
            moving[1].x = x
            val cached = solver.solve(poles, moving)
            val fresh = MagneticField.CoreSolver().solve(poles, moving)
            for (i in moving.indices) assertTrue(cached[i].distance(fresh[i]) < 1e-10)
        }
    }

    @Test
    fun `induced pole forces conserve force and torque and rotate with the contraption`() {
        val centers = listOf(Vector3d(1.0, .1, 0.0), Vector3d(2.0, .3, 0.0), Vector3d(2.0, 1.3, 0.0))
        val poles = magnet(0.0)
        val moments = MagneticField.CoreSolver().solve(poles, centers)
        val cores = centers.indices.map { MagneticField.corePoles(centers[it], moments[it]) }
        val groups = listOf(poles) + cores
        val total = Vector3d()
        val torque = Vector3d()
        for (i in groups.indices) for (j in i + 1 until groups.size) {
            for (a in groups[i]) for (b in groups[j]) {
                val f = MagneticField.pairForce(a, b)
                total.add(f).add(MagneticField.pairForce(b, a))
                torque.add(Vector3d(a.position).cross(f)).sub(Vector3d(b.position).cross(f))
            }
        }
        assertTrue(total.length() < 1e-8)
        assertTrue(torque.length() < 1e-7)
        val rotatedPoles = poles.map { MagneticField.Pole(Vector3d(it.position).rotateY(.7), it.charge) }
        val rotated = MagneticField.CoreSolver().solve(rotatedPoles, centers.map { Vector3d(it).rotateY(.7) })
        for (i in centers.indices) assertTrue(rotated[i].distance(Vector3d(moments[i]).rotateY(.7)) < 1e-8)
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
