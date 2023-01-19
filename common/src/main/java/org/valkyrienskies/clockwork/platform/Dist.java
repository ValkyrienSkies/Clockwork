package org.valkyrienskies.clockwork.platform;

import com.tterrag.registrate.util.entry.EntityEntry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueEntity;

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
