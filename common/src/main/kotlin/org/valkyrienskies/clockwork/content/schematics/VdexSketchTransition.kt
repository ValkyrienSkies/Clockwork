package org.valkyrienskies.clockwork.content.schematics

class VdexSketchTransition {
    enum class Phase { WAITING, DRAWING, READY, ERASING }
    var phase = Phase.WAITING
        private set
    var current: String? = null
        private set
    private var desired: String? = null
    private var started = 0L
    private var retained = 1f

    fun update(requested: String?, ready: Boolean, now: Long) {
        desired = requested?.takeUnless { it.isEmpty() }
        if (current != null && desired != current && phase != Phase.ERASING) {
            retained = draw(now)
            started = now
            phase = Phase.ERASING
        }
        if (phase == Phase.ERASING && elapsed(now, ERASE_MILLIS) >= 1f) {
            current = null
            phase = Phase.WAITING
        }
        if (phase == Phase.WAITING && desired != null && ready) {
            current = desired
            started = now
            phase = Phase.DRAWING
        }
        if (phase == Phase.DRAWING && elapsed(now, DRAW_MILLIS) >= 1f) phase = Phase.READY
    }

    fun draw(now: Long) = when (phase) {
        Phase.WAITING -> 0f
        Phase.DRAWING -> elapsed(now, DRAW_MILLIS)
        Phase.ERASING -> retained
        Phase.READY -> 1f
    }

    fun erase(now: Long) = if (phase == Phase.ERASING) elapsed(now, ERASE_MILLIS) else 0f
    private fun elapsed(now: Long, duration: Long) = ((now - started).toFloat() / duration).coerceIn(0f, 1f)

    companion object {
        const val DRAW_MILLIS = 1450L
        const val ERASE_MILLIS = 650L
    }
}
