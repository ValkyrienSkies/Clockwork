package org.valkyrienskies.clockwork.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.CreativeModeTab;

public class SharedValues {

    @ExpectPlatform
    public static CreativeModeTab creativeTab() {
        throw new AssertionError();
    }

}
