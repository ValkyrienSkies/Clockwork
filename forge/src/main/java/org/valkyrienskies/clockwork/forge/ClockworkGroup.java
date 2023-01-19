package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.content.AllSections;
import net.minecraft.world.item.ItemStack;
import org.valkyrienskies.clockwork.ClockWorkBlocks;

import java.util.EnumSet;

public class ClockworkGroup extends ClockworkGroupBase {

    public ClockworkGroup() {
        super("base");
    }

    @Override
    protected EnumSet<AllSections> getSections() {
        return EnumSet.complementOf(EnumSet.of(AllSections.PALETTES));
    }

    @Override
    public ItemStack makeIcon() {
        return ClockWorkBlocks.PHYSICS_INFUSER.asStack();
    }
}
