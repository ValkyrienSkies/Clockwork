package org.valkyrienskies.clockwork.client.render.goggles

import org.joml.Matrix4fc
import org.joml.Vector3dc
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object AeronautHudMath {
    private val minimumAimAlignment = cos(Math.toRadians(15.0))

    data class Point(val x: Float, val y: Float)

    internal fun nearAimAlignment(offset: Vector3dc, look: Vector3dc, range: Double): Double? {
        val distanceSquared = offset.lengthSquared()
        if (distanceSquared > range * range) return null
        return (offset.dot(look) / sqrt(distanceSquared)).takeIf { it >= minimumAimAlignment }
    }

    fun project(relative: Vector3dc, matrix: Matrix4fc, width: Int, height: Int): Point? {
        if (!relative.isFinite || width <= 0 || height <= 0) return null
        val clip = matrix.transform(Vector4f(relative.x().toFloat(), relative.y().toFloat(), relative.z().toFloat(), 1f))
        if (!clip.isFinite || clip.w <= 0.001f || clip.z < -clip.w || clip.z > clip.w) return null
        val x = (clip.x / clip.w + 1f) * width / 2f
        val y = (1f - clip.y / clip.w) * height / 2f
        return if (x in -32f..width + 32f && y in -32f..height + 32f) Point(x, y) else null
    }

    /** Direction may be scaled by a ship's inverse transform; t stays in world-space metres. */
    fun rayEntry(origin: Vector3dc, direction: Vector3dc, minimum: Vector3dc, maximum: Vector3dc, range: Double): Double? {
        if (!origin.isFinite || !direction.isFinite || !minimum.isFinite || !maximum.isFinite) return null
        var near = 0.0
        var far = range
        for (axis in 0..2) {
            val d = direction.get(axis)
            val o = origin.get(axis)
            if (abs(d) < 1e-12) {
                if (o < minimum.get(axis) || o > maximum.get(axis)) return null
            } else {
                val a = (minimum.get(axis) - o) / d
                val b = (maximum.get(axis) - o) / d
                near = max(near, min(a, b))
                far = min(far, max(a, b))
                if (near > far) return null
            }
        }
        return near
    }
}
