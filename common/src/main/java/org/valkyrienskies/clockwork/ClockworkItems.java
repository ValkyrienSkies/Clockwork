package org.valkyrienskies.clockwork;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;
import org.valkyrienskies.clockwork.content.curiosities.tools.bluperglue.BluperGlueItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer;

import static com.simibubi.create.content.AllSections.MATERIALS;
import static org.valkyrienskies.clockwork.ClockWorkMod.REGISTRATE;
public class ClockworkItems {
        static {
            REGISTRATE.creativeModeTab(() -> ClockWorkMod.BASE_CREATIVE_TAB);
        }

        static {
            REGISTRATE.startSection(MATERIALS);
        }

        public static final ItemEntry<Item> BLUUGUU = ingredient("bluuguu");

        static {
            REGISTRATE.startSection(AllSections.KINETICS);
        }

        static {
            REGISTRATE.startSection(AllSections.CURIOSITIES);
        }

        public static final ItemEntry<BluperGlueItem> BLUPERGLUE = REGISTRATE.item("bluperglue", BluperGlueItem::new)
                .properties(p -> p.stacksTo(1)
                        .durability(99))
                .register();

        public static final ItemEntry<GravitronItem> GRAVITRON = REGISTRATE.item("gravitron", GravitronItem::new)
                .properties(p -> p.stacksTo(1))
                .transform(CreateRegistrate.customRenderedItem(() -> GravitronItemRenderer::new))
                .tag(AllTags.AllItemTags.WRENCH.tag)
                .model(AssetLookup.itemModelWithPartials())
                .register();


        //Shortcuts

        private static ItemEntry<Item> ingredient(String name) {
            return REGISTRATE.item(name, Item::new)
                    .register();
        }

        public static void register() {}
}
