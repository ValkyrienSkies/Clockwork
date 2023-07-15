package org.valkyrienskies.clockwork;

import com.simibubi.create.infrastructure.item.CreateCreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ClockWorkTab extends CreateCreativeModeTab {
    public ClockWorkTab() {
        super("clockwork");
    }

    @Override
    public ItemStack makeIcon() {
        return ClockWorkBlocks.PHYSICS_INFUSER.asStack();
    }
}
