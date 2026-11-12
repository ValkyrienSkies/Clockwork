package org.valkyrienskies.clockwork.content.kinetics.combobulator

internal data class ThinkablePulse(val id: Long, val startedAt: Long, val duration: Int) {
    fun progress(now: Long, partialTick: Float = 0f): Double =
        if (id < 0 || duration <= 0) -1.0 else (now - startedAt + partialTick.toDouble()) / duration

    companion object {
        fun receive(previous: ThinkablePulse?, id: Long, age: Int, duration: Int, now: Long): ThinkablePulse {
            if (id < 0 || age < 0 || duration !in 1..80) return ThinkablePulse(-1, now, 0)
            if (previous?.id == id) return previous
            return ThinkablePulse(id, now - age, duration)
        }
    }
}
