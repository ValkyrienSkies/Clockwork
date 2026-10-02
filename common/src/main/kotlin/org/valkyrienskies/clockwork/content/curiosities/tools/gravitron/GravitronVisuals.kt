package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import kotlin.math.abs
import kotlin.math.exp

/** Shared palette, model coordinates (pixels), and surface envelopes. */
object GravitronVisuals {
    const val WANDERLITE = 0xC3A0E3
    const val WANDERLITE_LIGHT = 0xF7CFEF
    const val EMITTER_X = 7.70161f
    const val EMITTER_Y = 5.85355f
    const val EMITTER_Z = 0.65f
    const val CORE_SCALE = 0.18f
    const val DIAL_X = 4.50835f
    const val DIAL_Y = 6.52543f
    const val DIAL_Z = 14.16f
    const val SURFACE_RADIUS = 16

    fun coreAngle(ticks: Float): Float = (ticks * 2f) % 360f

    fun surfaceStrength(action: GravitronAction, distance: Float, age: Float): Float {
        val edge = 1f - GravitronAnimation.smooth((distance - SURFACE_RADIUS + 3f) / 3f)
        val strength = when (action) {
            GravitronAction.GRAB, GravitronAction.HOLD -> {
                val radius = (age * 0.22f) % 6f
                0.36f * band(distance, radius, 0.85f) * exp(-distance * 0.22f) *
                    GravitronAnimation.smooth(age / 4f)
            }
            GravitronAction.LAUNCH -> band(distance, age * 0.8f, 1.1f) *
                (1f - GravitronAnimation.smooth((age - 16f) / 12f))
            GravitronAction.FREEZE -> GravitronAnimation.smooth((age * 0.6f - distance) / 1.4f) *
                (1f - GravitronAnimation.smooth((age - 85f) / 35f)) * 0.88f
            GravitronAction.UNFREEZE -> band(distance, (18f - age * 0.8f).coerceAtLeast(0f), 1.5f) *
                (1f - GravitronAnimation.smooth((age - 20f) / 8f)) * 0.7f
            GravitronAction.RELEASE -> band(distance, age * 0.28f, 0.7f) *
                (1f - GravitronAnimation.smooth(age / 14f)) * 0.24f
            else -> 0f
        }
        return (strength * edge).coerceIn(0f, 1f)
    }

    private fun band(distance: Float, radius: Float, width: Float): Float =
        1f - GravitronAnimation.smooth(abs(distance - radius) / width)
}
