package org.valkyrienskies.clockwork.platform.api

import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.Registry
import java.util.function.Supplier

interface DeferredRegister<T> {
    fun register(id: String?, value: Supplier<T>?)
    fun registerAll()

    companion object {
        @ExpectPlatform
        fun <T> create(registry: Registry<T>, mod_id: String?): DeferredRegister<T> {
            throw AssertionError()
        }
    }
}