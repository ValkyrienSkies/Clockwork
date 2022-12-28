package org.valkyrienskies.clockwork.forge.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.forge.DeferredRegisterImpl;
import org.valkyrienskies.clockwork.registry.DeferredRegister;
import org.valkyrienskies.clockwork.services.DeferredRegisterBackend;

public class DeferredRegisterBackendForge implements DeferredRegisterBackend {

    @NotNull
    @Override
    public <T> DeferredRegister<T> makeDeferredRegister(@NotNull String id, @NotNull ResourceKey<Registry<T>> registry) {
        return new DeferredRegisterImpl(id, registry);
    }
}
