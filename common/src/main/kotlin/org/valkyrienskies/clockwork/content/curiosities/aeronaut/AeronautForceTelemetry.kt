package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import java.util.concurrent.ConcurrentHashMap

/** Only subscribed ships publish immutable physics readouts. Nothing here serializes with a ship. */
object AeronautForceTelemetry {
    private class Subscription {
        @Volatile var expires = 0L
        val samples = ConcurrentHashMap<String, Sample>()
        val nextSample = ConcurrentHashMap<String, Long>()
    }
    private data class Sample(val time: Long, val center: AeronautForceCenter?)
    private val ships = ConcurrentHashMap<Long, Subscription>()
    private const val INTERVAL = 200_000_000L
    private const val EXPIRY = 1_000_000_000L

    fun watch(shipId: Long) {
        val now = System.nanoTime()
        ships.entries.removeIf { it.value.expires < now }
        ships.computeIfAbsent(shipId) { Subscription() }.expires = now + EXPIRY
    }

    fun begin(shipId: Long, source: String): AeronautForceAccumulator? {
        val subscription = ships[shipId] ?: return null
        val now = System.nanoTime()
        if (subscription.expires < now || now < (subscription.nextSample[source] ?: 0L)) return null
        subscription.nextSample[source] = now + INTERVAL
        return AeronautForceAccumulator()
    }

    fun publish(shipId: Long, source: String, forces: AeronautForceAccumulator?) {
        if (forces == null) return
        ships[shipId]?.samples?.set(source, Sample(System.nanoTime(), forces.center()))
    }

    fun thrust(shipId: Long): AeronautForceCenter? {
        val result = AeronautForceAccumulator()
        val now = System.nanoTime()
        ships[shipId]?.let { subscription ->
            subscription.samples.entries.removeIf { now - it.value.time >= EXPIRY }
            subscription.nextSample.keys.retainAll(subscription.samples.keys)
            subscription.samples.values.forEach { result.add(it.center) }
        }
        return result.center()
    }

    fun clear() = ships.clear()
}
