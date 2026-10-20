package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import kotlin.math.sin

data class InfuserSequence(val startedAt: Long, val firstUse: Boolean, var skippedAt: Int = -1) {
    val naturalImpact get() = if (firstUse) 220 else 80
    val impact get() = if (skippedAt >= 0) minOf(naturalImpact, skippedAt + 6) else naturalImpact
    val settled get() = impact + if (firstUse && skippedAt < 0) 44 else 30
    val finished get() = impact + 100
    fun age(now: Long, partial: Float = 0f) = (now - startedAt).toFloat() + partial
    fun shouldImpact(now: Long, alreadyImpacted: Boolean) = !alreadyImpacted && age(now) >= impact

    fun skip(now: Long): Boolean {
        val elapsed = age(now).toInt().coerceAtLeast(0)
        if (elapsed >= impact || skippedAt >= 0) return false
        skippedAt = elapsed
        return true
    }

    fun connection(index: Int, count: Int): Float = if (!firstUse) 28f
        else 65f + index * (100f / count.coerceAtLeast(1))

    fun lift(age: Float): Float {
        val riseTime = if (firstUse) 140f else 54f
        fun rise(t: Float) = 2.8f * smooth((t - if (firstUse) 18f else 5f) / riseTime)
        val height = if (skippedAt >= 0) rise(minOf(age, skippedAt.toFloat())) else rise(age)
        if (age < impact) return height
        val descent = smooth((age - impact - 8f) / (settled - impact - 8f))
        return height * (1f - descent)
    }

    fun charge(age: Float) = if (age >= impact) (1f - (age - impact) / 24f).coerceIn(0f, 1f)
        else (age / impact.coerceAtLeast(1)).coerceIn(0f, 1f)

    companion object {
        fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
        // No render-time mutation or unbounded angles, including across the day boundary.
        fun angle(now: Long, partial: Float, axis: Int): Float {
            val period = longArrayOf(720, 480, 960)[axis]
            val phase = (now % period).toDouble() + partial
            return (phase * 360.0 / period + sin(phase * Math.PI * 2 / period) * 14).toFloat()
        }
    }
}
