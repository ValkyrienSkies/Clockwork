package org.valkyrienskies.clockwork.content.curiosities.magnet

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.sqrt

internal object MagneticField {
    const val RANGE = 16.0
    const val FORCE = 120000.0
    private const val SOFTENING_SQUARED = 0.0625
    private const val SATURATION_SQUARED = 4.0

    data class Pole(val position: Vector3d, val charge: Double)

    fun poles(center: Vector3dc, normal: Vector3dc, strength: Double): List<Pole> {
        val side = if (kotlin.math.abs(normal.x()) < 0.9) Vector3d(1.0, 0.0, 0.0) else Vector3d(0.0, 1.0, 0.0)
        val tangent = Vector3d(normal).cross(side).normalize()
        val other = Vector3d(normal).cross(tangent)
        return buildList {
            for (end in listOf(-1.0, 1.0)) for (u in listOf(-0.25, 0.25)) for (v in listOf(-0.25, 0.25)) {
                add(Pole(Vector3d(center).fma(end * 0.4, normal).fma(u, tangent).fma(v, other), end * strength / 4.0))
            }
        }
    }

    fun field(poles: List<Pole>, point: Vector3dc, result: Vector3d = Vector3d()): Vector3d {
        result.zero()
        for (pole in poles) {
            val x = point.x() - pole.position.x
            val y = point.y() - pole.position.y
            val z = point.z() - pole.position.z
            val factor = pole.charge * kernel(x * x + y * y + z * z)
            result.add(factor * x, factor * y, factor * z)
        }
        return result
    }

    fun pairForce(a: Pole, b: Pole): Vector3d {
        val r = Vector3d(a.position).sub(b.position)
        return r.mul(FORCE * a.charge * b.charge * kernel(r.lengthSquared()))
    }

    fun ironForce(pole: Pole, point: Vector3dc, totalField: Vector3dc, volume: Double): Vector3d {
        val r = Vector3d(point).sub(pole.position)
        val s = r.lengthSquared()
        if (s >= RANGE * RANGE) return Vector3d()
        val t = s + SOFTENING_SQUARED
        val taper = 1.0 - s / (RANGE * RANGE)
        val inverse = 1.0 / (t * sqrt(t))
        val a = taper * taper * taper * inverse
        val derivative = -3.0 * taper * taper * inverse / (RANGE * RANGE) - 1.5 * a / t
        return Vector3d(totalField).mul(a).fma(2.0 * derivative * r.dot(totalField), r)
            .mul(FORCE * volume * pole.charge / sqrt(1.0 + totalField.lengthSquared() / SATURATION_SQUARED))
    }

    private fun kernel(s: Double): Double {
        if (s >= RANGE * RANGE) return 0.0
        val taper = 1.0 - s / (RANGE * RANGE)
        val t = s + SOFTENING_SQUARED
        return taper * taper * taper / (t * sqrt(t))
    }
}
