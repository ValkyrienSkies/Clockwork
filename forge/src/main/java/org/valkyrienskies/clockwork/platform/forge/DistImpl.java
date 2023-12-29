package org.valkyrienskies.clockwork.platform.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class DistImpl {
    public static void onClient(Runnable runnable) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> runnable);
    }

    public static void onDedicatedServer(Runnable runnable) {
        DistExecutor.unsafeRunWhenOn(Dist.DEDICATED_SERVER, () -> runnable);
    }
}