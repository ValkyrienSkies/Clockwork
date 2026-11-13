package org.valkyrienskies.clockwork.compat.thinkable

import org.valkyrienskies.clockwork.api.thinkable.ThinkableContext
import org.valkyrienskies.clockwork.api.thinkable.ThinkableRegistry
import net.minecraft.world.level.block.state.BlockState
import java.util.function.Consumer

object ThinkableCompat {
    @Volatile private var providers = ThinkableProviderRegistry().also { it.freeze() }
    private var initialized = false
    @Volatile private var builtins = ThinkableProviderRegistry().also { it.freeze() }

    @JvmStatic @Synchronized
    fun initialize(register: Consumer<ThinkableRegistry>) {
        if (initialized) return
        val registry = ThinkableProviderRegistry()
        val defaults = ThinkableProviderRegistry()
        CreateThinkableProviders.register(defaults)
        defaults.freeze()
        register.accept(registry)
        registry.freeze()
        providers = registry
        builtins = defaults
        initialized = true
    }

    fun find(context: ThinkableContext) = providers.find(context) ?: builtins.find(context)
    fun supports(state: BlockState) = providers.supports(state) || builtins.supports(state)
}
