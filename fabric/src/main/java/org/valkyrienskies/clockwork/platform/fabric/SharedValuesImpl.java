package org.valkyrienskies.clockwork.platform.fabric;

import net.minecraft.world.item.CreativeModeTab;
import org.valkyrienskies.clockwork.fabric.ClockWorkGroup;

public class SharedValuesImpl {
    private static final CreativeModeTab TAB = new ClockWorkGroup();

    public static CreativeModeTab creativeTab() {
        return TAB;
    }

}
