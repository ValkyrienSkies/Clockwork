package org.valkyrienskies.clockwork.fabric;

import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.ClockWorkMod;
import org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker.PastrymakerItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker.PastrymakerItemRenderer;

import static com.simibubi.create.content.AllSections.MATERIALS;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class FabricClockworkItems {

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

    public static final ItemEntry<PastrymakerItem> PASTRYMAKER =
            REGISTRATE.item("pastrymaker", PastrymakerItem::new)
                    .transform(CreateRegistrate.customRenderedItem(() -> PastrymakerItemRenderer::new))
                    .model(AssetLookup.itemModelWithPartials())
                    .register();

    //Shortcuts

    private static ItemEntry<Item> ingredient(String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}
