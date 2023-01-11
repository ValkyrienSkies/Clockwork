package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.AllSections;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.fabric.content.curiosities.tools.bluperglue.BluperGlueItem;

import static com.simibubi.create.content.AllSections.MATERIALS;
import static org.valkyrienskies.clockwork.fabric.ClockWorkModFabric.REGISTRATE;

public class AllClockworkItems {
    public static final ItemEntry<Item> BLUUGUU = ingredient("bluuguu");
    public static final ItemEntry<BluperGlueItem> BLUPERGLUE = REGISTRATE.item("bluperglue", BluperGlueItem::new)
            .properties(p -> p.stacksTo(1)
                    .durability(99))
            .register();

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkModFabric.BASE_CREATIVE_TAB);
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
