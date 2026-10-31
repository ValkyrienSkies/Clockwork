package org.valkyrienskies.clockwork.util.render

import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/** Vertex-rate counterpart of clockwork_gravitron_surface for shader packs. */
object SurfaceEffectColor {
    fun luminance(r: Float, g: Float, b: Float) = r * 0.2126f + g * 0.7152f + b * 0.0722f

    // NativeImage uses ABGR, independently of the platform's byte order.
    fun greyPixel(abgr: Int): Int {
        val grey = luminance((abgr and 255).toFloat(), (abgr ushr 8 and 255).toFloat(),
            (abgr ushr 16 and 255).toFloat()).toInt().coerceIn(0, 255)
        return (abgr and -0x1000000) or (grey * 0x010101)
    }

    fun shade(mode: Int, distance: Float, time: Float, r: Float, g: Float, b: Float, alpha: Float,
              out: Vector4f): Vector4f {
        out.set(r, g, b, alpha)
        when (mode) {
            1 -> {
                val grey = luminance(r, g, b)
                // A small stepped flicker preserves the time-stopped feel without fragment noise.
                val flicker = 0.96f + 0.04f * sin(distance * 2.1f + floor(time * 0.35f))
                out.set(grey, grey, grey, alpha * flicker)
            }
            3 -> {
                val wave = smooth(0.5f + 0.5f * sin(distance * 0.85f - time * 0.12f))
                out.set(r * (0.95f + 0.25f * wave), g * (0.95f + 0.25f * wave),
                    b * (0.95f + 0.25f * wave), alpha * (0.3f + 0.65f * wave))
            }
            4 -> {
                val front = 1f - smooth((abs(distance) - 0.15f) / 1.15f)
                val wake = if (distance <= 0f) 1f - smooth(-distance / 2.8f) else 0f
                out.set(r * 1.25f * (1 - front) + 0.98f * front,
                    g * 1.25f * (1 - front) + 0.88f * front,
                    b * 1.25f * (1 - front) + front, alpha * (front * 0.95f + wake * 0.18f))
            }
            else -> {
                // Broad interference survives vertex interpolation; subpixel shader noise does not.
                val wave = smooth(0.5f + 0.5f * sin(distance * 1.7f - time * 0.13f))
                val brightness = if (mode == 2) 1.35f else 1f
                out.set((r + 0.18f * wave) * brightness, (g + 0.1f * wave) * brightness,
                    (b + 0.22f * wave) * brightness,
                    alpha * if (mode == 2) (0.5f + 0.65f * wave) else (0.28f + 0.55f * wave))
            }
        }
        // Entity colors are bytes. Values over 1 must saturate, never wrap to another hue.
        return out.set(out.x.coerceIn(0f, 1f), out.y.coerceIn(0f, 1f),
            out.z.coerceIn(0f, 1f), out.w.coerceIn(0f, 1f))
    }

    private fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
}
