package org.valkyrienskies.clockwork.platform.api.fabric;

import io.github.fabricators_of_create.porting_lib.util.LazyRegistrar;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class DeferredRegisterImpl<T> implements org.valkyrienskies.clockwork.platform.api.DeferredRegister<T> {
    private final Registry<T> registry;
    private final String modId;

    private DeferredRegisterImpl(Registry<T> registry, String modId) {
        this.registry = registry;
        this.modId = modId;
    }

    public static <T> org.valkyrienskies.clockwork.platform.api.DeferredRegister<T> create(Registry<T> registry, String mod_id) {
        return new DeferredRegisterImpl<T>(registry, mod_id);
    }

    @Override
    public void register(String id, Supplier<T> value) {
        Registry.register(registry, new ResourceLocation(modId, id), value.get());
    }

    @Override
    public void registerAll() {

    }

}
