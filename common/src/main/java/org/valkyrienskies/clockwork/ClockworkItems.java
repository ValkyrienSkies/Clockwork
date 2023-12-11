package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.valkyrienskies.clockwork.content.curiosities.tools.BluperGlueItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.ShipDestroyerItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;

public class ClockworkItems {

    static {
    }

    public static final ItemEntry<BluperGlueItem> BLUPERGLUE =
            ClockworkMod.INSTANCE.getREGISTRATE().item("bluperglue", BluperGlueItem::new)
                    .properties(p -> p.rarity(Rarity.UNCOMMON))
                    .properties(p -> p.stacksTo(1))
                    .properties(p -> p.durability(1000))
                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
                    .register();

    public static final ItemEntry<ShipDestroyerItem> SHIP_DESTROYER =
            ClockworkMod.INSTANCE.getREGISTRATE().item("ship_destroyer", ShipDestroyerItem::new)
                    .properties(p -> p.rarity(Rarity.UNCOMMON))
                    .properties(p -> p.stacksTo(1))
                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
                    .register();


    public static final ItemEntry<GravitronItem> GRAVITRON = ClockworkMod.INSTANCE.getREGISTRATE().item("gravitron", GravitronItem::new)
            .properties(p -> p.stacksTo(1)).properties(p -> p.rarity(Rarity.UNCOMMON))
            .transform(ClockworkRegistrate.customRenderedItem(() -> GravitronItemRenderer::new))
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register();


    private static ItemEntry<Item> ingredient(String name) {
        return ClockworkMod.INSTANCE.getREGISTRATE().item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}