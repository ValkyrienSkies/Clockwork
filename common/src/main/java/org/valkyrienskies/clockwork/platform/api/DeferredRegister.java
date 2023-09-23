package org.valkyrienskies.clockwork.platform.api;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.Registry;

import java.util.function.Supplier;

public interface DeferredRegister<T> {

    @ExpectPlatform
    static <T> DeferredRegister<T> create(Registry<T> registry, String mod_id) {
        throw new AssertionError();
    }

    void register(String id, Supplier<T> value);

    void registerAll();
}