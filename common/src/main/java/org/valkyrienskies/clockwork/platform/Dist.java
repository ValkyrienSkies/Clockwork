package org.valkyrienskies.clockwork.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class Dist {
    @ExpectPlatform
    public static void onClient(Runnable runnable) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void onDedicatedServer(Runnable runnable) {
        throw new AssertionError();
    }
}
