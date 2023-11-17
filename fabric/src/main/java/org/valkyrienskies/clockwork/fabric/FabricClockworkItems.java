package org.valkyrienskies.clockwork.fabric;

import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.ClockworkMod;

public class FabricClockworkItems {

    static {
        //ClockworkMod.INSTANCE.getREGISTRATE().creativeModeTab(ClockworkMod.INSTANCE::getBASE_CREATIVE_TAB);
    }
    //Shortcuts

    private static ItemEntry<Item> ingredient(String name) {
        return ClockworkMod.INSTANCE.getREGISTRATE().item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}
