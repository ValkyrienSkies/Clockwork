package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import kotlin.math.PI
import kotlin.math.sin

/** Analytic, bounded phases: positive speed on every axis, independent of render count and casts. */
internal object WandCrystalMotion {
    fun angle(ticks: Long, partial: Float, axis: Int): Float {
        val time = (ticks % 24000).toDouble() + partial
        val phase = axis * 2.1
        val degrees = time * (2.4 + axis * 0.9) + (60 + axis * 20) * sin(time * 2 * PI / (240 + axis * 80) + phase) +
            20 * sin(time * 2 * PI / 600 + phase * 1.7)
        // Rounding to float can itself turn 359.999999... into 360.
        return ((degrees % 360 + 360) % 360).toFloat() % 360f
    }

    fun lift(ticks: Long, partial: Float) = 0.17f + sin(((ticks % 24000) + partial.toDouble()) * 2 * PI / 160).toFloat() * 0.035f
}
