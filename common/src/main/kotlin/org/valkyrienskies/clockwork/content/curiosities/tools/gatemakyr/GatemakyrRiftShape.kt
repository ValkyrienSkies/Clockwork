package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import kotlin.math.*

object GatemakyrRiftShape {
    data class Bounds(val minX: Double, val minY: Double, val maxX: Double, val maxY: Double)

    internal fun spreadDuration(width: Int, height: Int, impactX: Double, impactY: Double): Double {
        val w = width / 2.0
        val h = height / 2.0
        return 10 + 3 * maxOf(w + abs(impactX.coerceIn(-w, w)), h + abs(impactY.coerceIn(-h, h)))
    }

    fun sample(width: Int, height: Int, impactX: Double, impactY: Double, age: Float, surface: Boolean, closing: Boolean = false): Bounds {
        val w = width / 2.0; val h = height / 2.0
        if (closing) {
            val scale = GatemakyrAnimation.smooth(age / 16)
            return Bounds(-max(1.0 / 32, w * scale), -max(1.0 / 32, h * scale), max(1.0 / 32, w * scale), max(1.0 / 32, h * scale))
        }
        if (!surface) return Bounds(-w, -h, w, h)
        val x = impactX.coerceIn(-w, w); val y = impactY.coerceIn(-h, h)
        val reach = maxOf(w + abs(x), h + abs(y))
        val duration = spreadDuration(width, height, x, y).toFloat()
        val radius = max(1.0 / 32, reach * GatemakyrAnimation.smooth(age / duration))
        return Bounds(max(-w, floor((x - radius) * 16) / 16), max(-h, floor((y - radius) * 16) / 16),
            min(w, ceil((x + radius) * 16) / 16), min(h, ceil((y + radius) * 16) / 16))
    }
}
