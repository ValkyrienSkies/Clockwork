package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.content.curiosities.CombustibleItem;
import com.simibubi.create.foundation.data.AssetLookup;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer;
import org.valkyrienskies.clockwork.content.materials.solids.stratodonut.StratodonutItem;
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate;

import static com.simibubi.create.content.AllSections.MATERIALS;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;

public class ClockWorkItems {



    static {
        REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
    }

    static {
        REGISTRATE.startSection(MATERIALS);
    }
    public static final ItemEntry<Item> BLUUGUU = ingredient("bluuguu");
    public static final ItemEntry<StratodonutItem> STRATODONUT =
            REGISTRATE.item("stratodonut", StratodonutItem::new)
                    .properties(p -> p.rarity(Rarity.EPIC))
                    .properties(p -> p.stacksTo(4))
                    .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
                    .onRegister(i -> i.setBurnTime(Short.MAX_VALUE)) // fabric: furnaces are limited to Short values without Forge patches
                    .register();

    static {
        REGISTRATE.startSection(AllSections.KINETICS);
    }
    public static final ItemEntry<BluperGlueItem> BLUPERGLUE = REGISTRATE.item("bluperglue", BluperGlueItem::new)
            .properties(p -> p.stacksTo(1)
                    .durability(99))
            .register();

    static {
        REGISTRATE.startSection(AllSections.CURIOSITIES);
    }

    public static final ItemEntry<GravitronItem> GRAVITRON = REGISTRATE.item("gravitron", GravitronItem::new)
            .properties(p -> p.stacksTo(1)).properties(p -> p.rarity(Rarity.UNCOMMON))
            .transform(ClockworkRegistrate.customRenderedItem(() -> GravitronItemRenderer::new))
            .tag(AllTags.AllItemTags.WRENCH.tag)
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
