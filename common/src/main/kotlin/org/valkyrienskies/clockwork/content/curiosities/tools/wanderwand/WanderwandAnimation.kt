package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import kotlin.math.PI
import kotlin.math.sin

/** Tick-based envelopes. Rendering another camera or item never advances animation time. */
class WanderwandAnimation {
    var action = "draw"
        private set
    var started = 0L
        private set
    fun accept(action: String, now: Long) { this.action = action; started = now }
    fun age(now: Long, partial: Float) = (now - started).toFloat() + partial
    data class Pose(val lower: Float = 0f, val tilt: Float = 0f, val roll: Float = 0f, val charge: Float = 0f)
    fun sample(now: Long, partial: Float): Pose {
        val age = age(now, partial).coerceAtLeast(0f)
        if (action == "draw") {
            val t = (age / 16f).coerceIn(0f, 1f)
            val lower = (1 - t) * (1 - t) * (1 - t)
            return Pose(lower, -35 * lower, -22 * lower, sin(t * PI).toFloat() * 0.3f)
        }
        val t = (age / 22f).coerceIn(0f, 1f)
        val pulse = sin(t * PI).toFloat() * (1 - t)
        val inward = action in listOf("weld_start", "dismiss", "deselect_end", "cancel")
        val finish = action.endsWith("end") || action == "break"
        return Pose(0f, pulse * if (inward) -16f else if (finish) 23f else 12f,
            pulse * if (inward) -12f else 8f, pulse * if (finish) 1.4f else 0.8f)
    }
}
