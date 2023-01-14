package org.valkyrienskies.clockwork.platform.forge;

import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.forge.ClockworkGroup;

public class SharedValuesImpl {
    private static final CreativeModeTab TAB = new ClockworkGroup();

    public static CreativeModeTab creativeTab() {
        return TAB;
    }

}
