package org.valkyrienskies.clockwork.content.physicalities.wheel

import org.joml.Vector3d

internal class WheelContactTimeline {
    private data class Sample(val tick: Long, val contacts: List<TireContact>)
    private val samples = ArrayDeque<Sample>()

    fun add(tick: Long, contacts: List<TireContact>): Boolean {
        val last = samples.lastOrNull()
        if (last != null && tick <= last.tick) return false
        if (last == null || tick - last.tick > 8) {
            samples.clear()
            samples.addLast(Sample(tick - CONTACT_INTERVAL, emptyList()))
        }
        if (samples.size == 8) samples.removeFirst()
        samples.addLast(Sample(tick, contacts))
        return true
    }

    fun sample(time: Double): List<TireContact> {
        val first = samples.firstOrNull() ?: return emptyList()
        if (time <= first.tick) return first.contacts
        var before = first
        for (i in 1 until samples.size) {
            val after = samples[i]
            if (time <= after.tick) {
                val t = ((time - before.tick) / (after.tick - before.tick)).toFloat()
                return blend(before.contacts, after.contacts, t)
            }
            before = after
        }
        val fade = ((time - before.tick - CONTACT_INTERVAL) / 4.0).coerceIn(0.0, 1.0).toFloat()
        return blend(before.contacts, emptyList(), fade)
    }

    private fun blend(from: List<TireContact>, to: List<TireContact>, t: Float): List<TireContact> {
        if (t <= 0f) return from
        if (t >= 1f) return to
        val previous = from.toMutableList()
        val result = to.map { next ->
            val old = previous.firstOrNull { it.surface == next.surface && it.normal.dot(next.normal) > .75 }
            previous.remove(old)
            TireContact(next.surface, old?.let { Vector3d(it.normal).lerp(next.normal, t.toDouble()).normalize() } ?: next.normal,
                (old?.compression ?: 0f) * (1 - t) + next.compression * t)
        }.toMutableList()
        previous.forEach { result.add(it.copy(compression = it.compression * (1 - t))) }
        return result.filter { it.compression > .00001f }.sortedByDescending { it.compression }.take(4)
    }
}
