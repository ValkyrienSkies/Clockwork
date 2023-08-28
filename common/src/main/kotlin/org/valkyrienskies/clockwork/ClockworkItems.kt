package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import com.simibubi.create.foundation.data.AssetLookup
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorItem
import org.valkyrienskies.clockwork.content.curiosities.tools.auric_designator.AreaDesignatorRenderer
import org.valkyrienskies.clockwork.content.materials.solids.stratodonut.StratodonutItem
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate

object ClockworkItems {
    init {
        REGISTRATE.creativeModeTab { ClockworkMod.BASE_CREATIVE_TAB }
    }

    val BLUUGUU = ingredient("bluuguu")
    val STRATODONUT: ItemEntry<StratodonutItem> = REGISTRATE.item("stratodonut") { StratodonutItem() }
        .properties { p -> p.rarity(Rarity.EPIC) }
        .properties { p -> p.stacksTo(4) }
        .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
        .onRegister { i -> i.setBurnTime(Short.MAX_VALUE) } // fabric: furnaces are limited to Short values without Forge patches
        .register()
    val AURIC_DESIGNATOR: ItemEntry<AreaDesignatorItem> = REGISTRATE.item("auric_designator") { AreaDesignatorItem() }
        .transform(ClockworkRegistrate.customRenderedItem { { AreaDesignatorRenderer() } })
        .properties { p -> p.rarity(Rarity.UNCOMMON) }
        .properties { p -> p.stacksTo(1) }
        .properties { p -> p.fireResistant() }
        .properties { p -> p.durability(1000) }
        .tag(AllTags.AllItemTags.UPRIGHT_ON_BELT.tag)
        .model(AssetLookup.itemModelWithPartials())
        .register()

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
    //    public static final ItemEntry<GravitronItem> GRAVITRON = REGISTRATE.item("gravitron", GravitronItem::new)
    //            .properties(p -> p.stacksTo(1)).properties(p -> p.rarity(Rarity.UNCOMMON))
    //            .transform(ClockworkRegistrate.customRenderedItem(() -> GravitronItemRenderer::new))
    //            .tag(AllTags.AllItemTags.WRENCH.tag)
    //            .model(AssetLookup.itemModelWithPartials())
    //            .register();
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
    private fun ingredient(name: String): ItemEntry<Item> {
        return REGISTRATE.item(name) { properties: Item.Properties -> Item(properties) }
            .register()
    }

    fun register() {}
}