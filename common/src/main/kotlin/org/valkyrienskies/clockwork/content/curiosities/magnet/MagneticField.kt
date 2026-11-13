package org.valkyrienskies.clockwork.content.curiosities.magnet

import org.joml.Vector3d
import org.joml.Vector3dc
import kotlin.math.sqrt
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max

internal object MagneticField {
    const val RANGE = 16.0
    const val FORCE = 120000.0
    private const val SOFTENING_SQUARED = 0.0625
    private const val LONG_RANGE_WEIGHT = 0.8
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

    fun coarsePoles(poles: List<Pole>): List<Pole> = listOf(-1.0, 1.0).map { sign ->
        val center = Vector3d()
        var charge = 0.0
        for (pole in poles) if (pole.charge * sign > 0.0) {
            center.fma(pole.charge, pole.position)
            charge += pole.charge
        }
        Pole(if (charge != 0.0) center.div(charge) else center, charge)
    }

    fun coarseWeight(distanceSquared: Double, radius: Double): Double {
        if (radius <= 0.0) return 0.0
        val relative = distanceSquared / (radius * radius)
        val blend = ((relative - 16.0) / 20.0).coerceIn(0.0, 1.0)
        return blend * blend * (3.0 - 2.0 * blend)
    }

    fun field(poles: List<Pole>, point: Vector3dc, result: Vector3d = Vector3d()): Vector3d {
        result.zero()
        for (i in poles.indices) {
            val pole = poles[i]
            val x = point.x() - pole.position.x
            val y = point.y() - pole.position.y
            val z = point.z() - pole.position.z
            val factor = pole.charge * kernel(x * x + y * y + z * z)
            result.add(factor * x, factor * y, factor * z)
        }
        return result
    }

    fun pairForce(a: Pole, b: Pole): Vector3d = pairForce(a, b, Vector3d())

    fun pairForce(a: Pole, b: Pole, result: Vector3d): Vector3d {
        result.set(a.position).sub(b.position)
        return result.mul(FORCE * a.charge * b.charge * kernel(result.lengthSquared()))
    }

    fun corePoles(center: Vector3dc, moment: Vector3dc, radius: Double = .4): List<Pole> {
        val length = moment.length()
        if (length < 1e-10) return emptyList()
        val offset = Vector3d(moment).mul(radius / length)
        val charge = length / (2.0 * radius)
        return listOf(Pole(Vector3d(center).sub(offset), -charge), Pole(Vector3d(center).add(offset), charge))
    }

    class CoreSolver {
        private data class Cell(val x: Int, val y: Int, val z: Int)
        private class Link(val a: Int, val b: Int, var xx: Double = 0.0, var yy: Double = 0.0, var zz: Double = 0.0,
                           var xy: Double = 0.0, var xz: Double = 0.0, var yz: Double = 0.0)
        private var positions = emptyList<Vector3d>()
        private var sources = emptyList<Pole>()
        private var neighbourhood = emptyList<Vector3d>()
        private val links = ArrayList<Link>()
        private var susceptibility = DoubleArray(0)
        private var excitation = DoubleArray(0)
        private var current = DoubleArray(0)
        private var next = DoubleArray(0)
        private var field = DoubleArray(0)
        var moments = emptyList<Vector3d>()
            private set
        var iterations = 0
            private set

        fun solve(poles: List<Pole>, centers: List<Vector3d>): List<Vector3d> {
            val geometryChanged = centers.size != positions.size || centers.indices.any { !centers[it].equals(positions[it], 1e-8) }
            val fieldChanged = poles.size != sources.size || poles.indices.any {
                poles[it].charge != sources[it].charge || !poles[it].position.equals(sources[it].position, 1e-8)
            }
            if (!geometryChanged && !fieldChanged) return moments
            if (geometryChanged) prepare(centers)
            if (sources.size != poles.size || poles.indices.any { poles[it].charge != sources[it].charge }) {
                sources = poles.map { Pole(Vector3d(it.position), it.charge) }
            } else for (i in poles.indices) sources[i].position.set(poles[i].position)
            val value = Vector3d()
            for (i in centers.indices) {
                field(poles, centers[i], value)
                excitation[i * 3] = value.x; excitation[i * 3 + 1] = value.y; excitation[i * 3 + 2] = value.z
            }
            current.fill(0.0)
            iterations = 0
            for (iteration in 0 until 32) {
                excitation.copyInto(field)
                for (link in links) {
                    val a = link.a * 3; val b = link.b * 3
                    val ax = current[a]; val ay = current[a + 1]; val az = current[a + 2]
                    val bx = current[b]; val by = current[b + 1]; val bz = current[b + 2]
                    field[a] += link.xx * bx + link.xy * by + link.xz * bz
                    field[a + 1] += link.xy * bx + link.yy * by + link.yz * bz
                    field[a + 2] += link.xz * bx + link.yz * by + link.zz * bz
                    field[b] += link.xx * ax + link.xy * ay + link.xz * az
                    field[b + 1] += link.xy * ax + link.yy * ay + link.yz * az
                    field[b + 2] += link.xz * ax + link.yz * ay + link.zz * az
                }
                var error = 0.0
                for (i in centers.indices) {
                    val offset = i * 3
                    val x = field[offset] * susceptibility[i]
                    val y = field[offset + 1] * susceptibility[i]
                    val z = field[offset + 2] * susceptibility[i]
                    val saturation = 1.0 / sqrt(1.0 + (x * x + y * y + z * z) / 2.25)
                    next[offset] = x * saturation; next[offset + 1] = y * saturation; next[offset + 2] = z * saturation
                    for (axis in 0..2) error = max(error, abs(next[offset + axis] - current[offset + axis]))
                }
                val swap = current; current = next; next = swap
                iterations = iteration + 1
                if (error < 1e-7) break
            }
            for (i in centers.indices) moments[i].set(current[i * 3], current[i * 3 + 1], current[i * 3 + 2])
            return moments
        }

        private fun prepare(centers: List<Vector3d>) {
            if (centers.size != positions.size) {
                positions = centers.map(::Vector3d)
                moments = List(centers.size) { Vector3d() }
                susceptibility = DoubleArray(centers.size)
                excitation = DoubleArray(centers.size * 3)
                current = DoubleArray(excitation.size); next = DoubleArray(excitation.size); field = DoubleArray(excitation.size)
            } else for (i in centers.indices) positions[i].set(centers[i])
            if (neighbourhood.size != centers.size || centers.indices.any { centers[it].distanceSquared(neighbourhood[it]) > .0625 }) {
                neighbourhood = centers.map(::Vector3d)
                links.clear()
                fun cell(p: Vector3d) = Cell(floor(p.x / 5.0).toInt(), floor(p.y / 5.0).toInt(), floor(p.z / 5.0).toInt())
                val cells = centers.indices.groupBy { cell(centers[it]) }
                for (i in centers.indices) {
                    val key = cell(centers[i])
                    for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                        val neighbours = cells[Cell(key.x + dx, key.y + dy, key.z + dz)] ?: continue
                        for (j in neighbours) if (j > i && centers[i].distanceSquared(centers[j]) <= 20.25) links.add(Link(i, j))
                    }
                }
            }
            susceptibility.fill(0.0)
            for (link in links) {
                val x = centers[link.a].x - centers[link.b].x
                val y = centers[link.a].y - centers[link.b].y
                val z = centers[link.a].z - centers[link.b].z
                val s = x * x + y * y + z * z
                if (s >= 16.0) {
                    link.xx = 0.0; link.yy = 0.0; link.zz = 0.0; link.xy = 0.0; link.xz = 0.0; link.yz = 0.0
                    continue
                }
                val t = s + SOFTENING_SQUARED
                val inv = 1.0 / t
                val taper = 1.0 - s / (RANGE * RANGE)
                val base = (sqrt(inv) + LONG_RANGE_WEIGHT) * inv
                val k = taper * taper * taper * base
                val derivative = -3.0 * taper * taper * base / (RANGE * RANGE) +
                    taper * taper * taper * (-1.5 * sqrt(inv) - LONG_RANGE_WEIGHT) * inv * inv
                val blend = ((16.0 - s) / 12.0).coerceIn(0.0, 1.0)
                val weight = blend * blend * (3.0 - 2.0 * blend)
                val a = -k * weight
                val b = -2.0 * derivative * weight
                link.xx = a + b * x * x; link.yy = a + b * y * y; link.zz = a + b * z * z
                link.xy = b * x * y; link.xz = b * x * z; link.yz = b * y * z
                val bound = max(abs(a), abs(a + b * s))
                susceptibility[link.a] += bound; susceptibility[link.b] += bound
            }
            for (i in susceptibility.indices) susceptibility[i] = minOf(.45, .85 / susceptibility[i].coerceAtLeast(1e-12))
        }
    }

    class Sample {
        val field = Vector3d()
        var coefficients = DoubleArray(0)
        var response = 0.0
    }

    fun sample(poles: List<Pole>, point: Vector3dc, result: Sample) {
        if (result.coefficients.size < poles.size * 5) result.coefficients = DoubleArray(poles.size * 10)
        result.field.zero()
        for (i in poles.indices) {
            val pole = poles[i]
            val x = point.x() - pole.position.x
            val y = point.y() - pole.position.y
            val z = point.z() - pole.position.z
            val s = x * x + y * y + z * z
            val offset = i * 5
            val coefficients = result.coefficients
            coefficients[offset] = x; coefficients[offset + 1] = y; coefficients[offset + 2] = z
            if (s >= RANGE * RANGE) {
                coefficients[offset + 3] = 0.0; coefficients[offset + 4] = 0.0
                continue
            }
            val t = s + SOFTENING_SQUARED
            val inverseT = 1.0 / t
            val inverseRoot = sqrt(inverseT)
            val taper = 1.0 - s / (RANGE * RANGE)
            val base = (inverseRoot + LONG_RANGE_WEIGHT) * inverseT
            val a = pole.charge * taper * taper * taper * base
            val derivative = pole.charge * (-3.0 * taper * taper * base / (RANGE * RANGE) +
                taper * taper * taper * (-1.5 * inverseRoot - LONG_RANGE_WEIGHT) * inverseT * inverseT)
            coefficients[offset + 3] = a; coefficients[offset + 4] = derivative
            result.field.add(x * a, y * a, z * a)
        }
        result.response = FORCE / sqrt(1.0 + result.field.lengthSquared() / SATURATION_SQUARED)
    }

    fun ironForce(sample: Sample, index: Int, volume: Double, result: Vector3d): Vector3d {
        val offset = index * 5
        val c = sample.coefficients
        val x = c[offset]; val y = c[offset + 1]; val z = c[offset + 2]
        val a = c[offset + 3]
        val along = 2.0 * c[offset + 4] * (x * sample.field.x + y * sample.field.y + z * sample.field.z)
        return result.set(sample.field.x * a + x * along, sample.field.y * a + y * along, sample.field.z * a + z * along)
            .mul(volume * sample.response)
    }

    fun ironForce(pole: Pole, point: Vector3dc, totalField: Vector3dc, volume: Double): Vector3d {
        val r = Vector3d(point).sub(pole.position)
        val s = r.lengthSquared()
        if (s >= RANGE * RANGE) return Vector3d()
        val t = s + SOFTENING_SQUARED
        val inverseT = 1.0 / t
        val inverseRoot = sqrt(inverseT)
        val taper = 1.0 - s / (RANGE * RANGE)
        val base = (inverseRoot + LONG_RANGE_WEIGHT) * inverseT
        val a = taper * taper * taper * base
        val derivative = -3.0 * taper * taper * base / (RANGE * RANGE) +
            taper * taper * taper * (-1.5 * inverseRoot - LONG_RANGE_WEIGHT) * inverseT * inverseT
        return Vector3d(totalField).mul(a).fma(2.0 * derivative * r.dot(totalField), r)
            .mul(FORCE * volume * pole.charge / sqrt(1.0 + totalField.lengthSquared() / SATURATION_SQUARED))
    }

    private fun kernel(s: Double): Double {
        if (s >= RANGE * RANGE) return 0.0
        val taper = 1.0 - s / (RANGE * RANGE)
        val t = s + SOFTENING_SQUARED
        val inverseT = 1.0 / t
        return taper * taper * taper * (sqrt(inverseT) + LONG_RANGE_WEIGHT) * inverseT
    }
}
