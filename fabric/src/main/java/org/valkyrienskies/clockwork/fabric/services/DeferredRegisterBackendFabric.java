package org.valkyrienskies.clockwork.fabric.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.clockwork.fabric.DeferredRegisterImpl;
import org.valkyrienskies.clockwork.registry.DeferredRegister;
import org.valkyrienskies.clockwork.services.DeferredRegisterBackend;

public class DeferredRegisterBackendFabric implements DeferredRegisterBackend {

    @NotNull
    @Override
    public <T> DeferredRegister<T> makeDeferredRegister(@NotNull String id, @NotNull ResourceKey<Registry<T>> registry) {
        return new DeferredRegisterImpl<>(id, registry);
    }
}
