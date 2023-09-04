package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.infrastructure.item.CreateCreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockworkBlocks;

public class ClockworkGroup extends CreateCreativeModeTab {

    public ClockworkGroup() {
        super("clockwork");
    }

    @Override
    public ItemStack makeIcon() {
        return ClockworkBlocks.PHYSICS_INFUSER.asStack();
    }
}
