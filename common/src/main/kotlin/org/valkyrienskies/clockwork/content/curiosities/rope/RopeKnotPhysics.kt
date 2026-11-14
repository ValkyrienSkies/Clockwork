package org.valkyrienskies.clockwork.content.curiosities.rope

import kotlin.math.max

object RopeKnotPhysics {
    const val STIFFNESS = 400.0
    const val DAMPING = 32.0
    const val BREAK_ACCELERATION = 3200.0
    const val UNTYING_HITS = 3
    const val HIT_INTERVAL = 6L

    fun stressRatio(stretch: Double, speed: Double): Double = if (stretch < 0) 0.0 else
        (max(0.0, stretch) * STIFFNESS + max(0.0, speed) * DAMPING) / BREAK_ACCELERATION

    fun overloaded(stretch: Double, speed: Double): Boolean = stressRatio(stretch, speed) > 1.0

    fun placedLength(distance: Double) = (distance + 0.25).coerceIn(0.5, 128.0)
    fun breaksOnHit(hits: Int, cutting: Boolean) = cutting || hits >= UNTYING_HITS
}
