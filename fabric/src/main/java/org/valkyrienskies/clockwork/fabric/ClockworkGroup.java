package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.AllSections;
import net.minecraft.world.item.ItemStack;

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
        return AllClockworkBlocks.PROPELLOR_BEARING.asStack();
    }
}
