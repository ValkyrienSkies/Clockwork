package org.valkyrienskies.clockwork.effekseer.api.client.effekseer

import net.minecraft.client.Minecraft
import java.io.Closeable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer


/**
 * @author ChloePrime
 */
abstract class SafeFinalized<T : Any> protected constructor(kept: T, private val closer: Consumer<T>) : Closeable {
    private val kept = AtomicReference(kept)

    init {
        KEEPER.add(kept)
    }

    @Suppress("deprecation")
    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            val kept = kept.get()
            if (kept != null) {
                Minecraft.getInstance().tell(Runnable { this.close() })
            }
        } finally {
        }
    }

    override fun close() {
        val removed = kept.getAndSet(null) ?: return
        try {
            closer.accept(removed)
        } finally {
            KEEPER.remove(removed)
        }
    }

    companion object {
        private val KEEPER: MutableSet<Any> = Collections.newSetFromMap(ConcurrentHashMap())
    }
}