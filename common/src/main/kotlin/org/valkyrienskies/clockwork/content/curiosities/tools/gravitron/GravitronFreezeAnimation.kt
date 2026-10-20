package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

/** A persistent field, followed by a thaw that starts from exactly the visible frozen coverage. */
class GravitronFreezeAnimation(private val startedAt: Long, private val initialAge: Float = 0f) {
    private var thawStartedAt: Long? = null
    private var ageAtThaw = 0f
    val thawing: Boolean get() = thawStartedAt != null

    fun freezeAge(now: Long, partialTick: Float = 0f): Float =
        if (thawing) ageAtThaw else (initialAge + (now - startedAt).toFloat() + partialTick).coerceIn(0f, 40f)

    fun thaw(now: Long) {
        if (thawing) return
        ageAtThaw = freezeAge(now)
        thawStartedAt = now
    }

    fun thawAge(now: Long, partialTick: Float = 0f): Float =
        thawStartedAt?.let { (now - it).toFloat() + partialTick } ?: 0f

    fun finished(now: Long): Boolean = thawing && thawAge(now) >= 28f
}
