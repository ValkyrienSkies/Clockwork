package org.valkyrienskies.clockwork.platform.fabric;

import com.tterrag.registrate.fabric.EnvExecutor;
import net.fabricmc.api.EnvType;

public class DistImpl {

    public static void onClient(Runnable runnable) {
        EnvExecutor.runWhenOn(EnvType.CLIENT, () -> runnable);
    }

    public static void onDedicatedServer(Runnable runnable) {
        EnvExecutor.runWhenOn(EnvType.SERVER, () -> runnable);
    }
}
