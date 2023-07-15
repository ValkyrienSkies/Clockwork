package org.valkyrienskies.clockwork.forge;

import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import org.valkyrienskies.clockwork.ClockWorkMod;

import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ForgeClockworkItems {
    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

//    public static final ItemEntry<PastrymakerItem> PASTRYMAKER =
//            REGISTRATE.item("pastrymaker", PastrymakerItem::new)
//                    .model(AssetLookup.itemModelWithPartials())
//                    .register();

    public static void register() {
    }
}
