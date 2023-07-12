package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.infrastructure.item.CreateCreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkBlocks;

import java.util.EnumSet;

public class ClockWorkGroup extends CreateCreativeModeTab {

    public ClockWorkGroup() {
        super("clockwork");
    }

    @Override
    public ItemStack makeIcon() {
        return ClockWorkBlocks.PHYSICS_INFUSER.asStack();
    }
}
