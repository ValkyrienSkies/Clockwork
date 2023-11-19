package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.valkyrienskies.clockwork.content.curiosities.tools.BluperGlueItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronItemRenderer;
import org.valkyrienskies.clockwork.renderer.AreaDesignatorRenderer;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;

public class ClockworkItems {

    static {
    }
    public static final ItemEntry<Item> BLUUGUU = ingredient("bluuguu");
    public static final ItemEntry<AreaDesignatorItem> AURIC_DESIGNATOR =
            ClockworkMod.INSTANCE.getREGISTRATE().item("auric_designator", AreaDesignatorItem::new)
                    .transform(ClockworkRegistrate.customRenderedItem(() -> AreaDesignatorRenderer::new))
                    .properties(p -> p.rarity(Rarity.UNCOMMON))
                    .properties(p -> p.stacksTo(1))
                    .properties(p -> p.fireResistant())
                    .properties(p -> p.durability(1000))
                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
                    .model(AssetLookup.itemModelWithPartials())
                    .register();

    public static final ItemEntry<BluperGlueItem> BLUPERGLUE =
            ClockworkMod.INSTANCE.getREGISTRATE().item("bluperglue", BluperGlueItem::new)
                    .properties(p -> p.rarity(Rarity.UNCOMMON))
                    .properties(p -> p.stacksTo(1))
                    .properties(p -> p.durability(1000))
                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
                    .register();

//    public static final ItemEntry<SodaBottleItem> EMPTY_SODA =
//            REGISTRATE.item("empty_soda", SodaBottleItem::new)
//                    .properties(p -> p.rarity(Rarity.COMMON))
//                    .properties(p -> p.stacksTo(1))
//                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
//                    .register();

//    public static final ItemEntry<BluperGlueItem> BLUPERGLUE = REGISTRATE.item("bluperglue", BluperGlueItem::new)
//            .properties(p -> p.stacksTo(1)
//                    .durability(99))
//            .register();


    public static final ItemEntry<GravitronItem> GRAVITRON = ClockworkMod.INSTANCE.getREGISTRATE().item("gravitron", GravitronItem::new)
            .properties(p -> p.stacksTo(1)).properties(p -> p.rarity(Rarity.UNCOMMON))
            .transform(ClockworkRegistrate.customRenderedItem(() -> GravitronItemRenderer::new))
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register();

//    public static final ItemEntry<WelderItem> WELDER = REGISTRATE.item("welder", WelderItem::new)
//            .properties(p -> p.stacksTo(1)).properties(p -> p.rarity(Rarity.RARE))
//            .transform(ClockworkRegistrate.customRenderedItem(() -> WelderItemRenderer::new))
//            .tag(AllTags.AllItemTags.WRENCH.tag)
//            .model(AssetLookup.itemModelWithPartials())
//            .register();


//    public static final ItemEntry<PastrymakerItem> PASTRYMAKER =
//            REGISTRATE.item("pastrymaker", PastrymakerItem::new)
//                    .transform(CreateRegistrate.customRenderedItem(() -> PastrymakerItemRenderer::new))
//                    .model(AssetLookup.itemModelWithPartials())
//                    .register();
    //Shortcuts

    private static ItemEntry<Item> ingredient(String name) {
        return ClockworkMod.INSTANCE.getREGISTRATE().item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}