package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.AllSections;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.ClockWorkMod;

import static com.simibubi.create.content.AllSections.MATERIALS;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class FabricClockworkItems {
    public static final ItemEntry<Item> BLUUGUU = ingredient("bluuguu");

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(MATERIALS);
    }

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }

    static {
        REGISTRATE.startSection(AllSections.CURIOSITIES);
    }

    //Shortcuts

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}
