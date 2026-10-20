package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs

/** Shared palette, model coordinates (pixels), and surface envelopes. */
object GravitronVisuals {
    const val WANDERLITE = 0xC3A0E3
    const val WANDERLITE_LIGHT = 0xF7CFEF
    const val EMITTER_X = 7.70161f
    const val EMITTER_Y = 5.85355f
    const val EMITTER_Z = 0.65f
    const val OVERCHARGE_X = 7.75f
    const val OVERCHARGE_Y = 6f
    const val OVERCHARGE_Z = 7.75f
    const val DIAL_X = 4.50835f
    const val DIAL_Y = 6.52543f
    const val DIAL_Z = 14.00144f
    const val SURFACE_RADIUS = 16

    fun coreAngle(ticks: Float): Float = (ticks * 2f) % 360f

    /** The exported needle already contains its -22.5 degree tilt; rotate about that plane's normal. */
    fun dialRotation(angle: Float): Quaternionf {
        val radians = (PI / 180).toFloat()
        val normal = Vector3f(0f, 0f, 1f).rotateX(-22.5f * radians)
        return Quaternionf().rotationAxis((10f - angle) * radians, normal.x, normal.y, normal.z)
    }

    fun surfaceStrength(action: GravitronAction, distance: Float, age: Float, frozenAge: Float = 40f): Float {
        val edge = 1f - GravitronAnimation.smooth((distance - SURFACE_RADIUS + 3f) / 3f)
        val strength = when (action) {
            GravitronAction.GRAB, GravitronAction.HOLD -> {
                val radius = (age * 0.22f) % 6f
                // Match the previous launch crest, retaining the slower, repeating grab motion.
                band(distance, radius, 1.1f) * GravitronAnimation.smooth(age / 4f)
            }
            GravitronAction.LAUNCH -> band(distance, age * 0.8f, 1.5f) *
                (1f - GravitronAnimation.smooth((age - 16f) / 12f))
            GravitronAction.FREEZE -> freezeFill(distance, age)
            // Keep everything inside the retreating front frozen, including a partially completed freeze.
            GravitronAction.UNFREEZE -> freezeFill(distance, frozenAge) *
                GravitronAnimation.smooth((18f - age * 0.8f - distance) / 1.4f)
            GravitronAction.RELEASE -> band(distance, age * 0.28f, 0.7f) *
                (1f - GravitronAnimation.smooth(age / 14f)) * 0.24f
            else -> 0f
        }
        return (strength * edge).coerceIn(0f, 1f)
    }

    private fun freezeFill(distance: Float, age: Float): Float =
        GravitronAnimation.smooth((age * 0.6f - distance) / 1.4f) * 0.88f

    private fun band(distance: Float, radius: Float, width: Float): Float =
        1f - GravitronAnimation.smooth(abs(distance - radius) / width)
}
