package org.valkyrienskies.clockwork.fabric;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.ClockWorkMod;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class FabricClockworkItems {

    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }
    //Shortcuts

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}
