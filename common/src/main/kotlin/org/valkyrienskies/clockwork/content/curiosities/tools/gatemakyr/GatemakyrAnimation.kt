package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import kotlin.math.PI
import kotlin.math.sin

class GatemakyrAnimation {
    var charging = false
        private set
    var side = 0
        private set
    var startedAt = 0L
        private set
    var firedAt = Long.MIN_VALUE
        private set
    private var releasedAt = Long.MIN_VALUE
    private var releasedCharge = 0f
    private var fired = false

    data class Pose(val charge: Float, val opening: Float, val recoil: Float, val energy: Float)

    fun start(endpoint: Int, now: Long) {
        if (charging && side == endpoint) return
        charging = true
        side = endpoint
        startedAt = now
        releasedAt = Long.MIN_VALUE
    }

    fun release(now: Long, fire: Boolean) {
        releasedCharge = sample(now, 0f).charge
        charging = false
        releasedAt = now
        fired = fire
        if (fire) firedAt = now
    }

    fun sample(now: Long, partialTick: Float): Pose {
        if (charging) {
            val charge = ((now - startedAt + partialTick) / CHARGE_TICKS).coerceIn(0f, 1f)
            return Pose(charge, 26f * smooth(charge), 0f, charge)
        }
        if (releasedAt == Long.MIN_VALUE) return Pose(0f, 0f, 0f, 0f)
        val age = (now - releasedAt + partialTick).coerceAtLeast(0f)
        val settle = 1f - smooth(age / 12f)
        val kick = if (fired && age < 10f) sin((age / 10f * PI).toFloat()) else 0f
        return Pose(releasedCharge * settle, 26f * smooth(releasedCharge) * settle + kick * 18f,
            kick, if (fired) 0f else releasedCharge * settle)
    }

    companion object {
        const val CHARGE_TICKS = 40f
        fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    }
}
