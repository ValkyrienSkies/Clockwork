package org.valkyrienskies.clockwork.platform.fabric;

import com.google.common.base.Suppliers;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class NativePlatformImpl {
    private static final Supplier<Boolean> IS_DATAGEN = Suppliers.memoize(
            () -> System.getProperty("fabric-api.datagen") != null
    );

    public static boolean isDataGen() {
        return IS_DATAGEN.get();
    }
}