package org.valkyrienskies.clockwork

import com.simibubi.create.AllItems
import com.simibubi.create.AllTags
import com.simibubi.create.foundation.data.AssetLookup
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.AuricCubeItemRenderer
import org.valkyrienskies.clockwork.content.AuricItem
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import java.util.function.Supplier

object ClockworkItems {



    @JvmField
    val GRAVITRON: ItemEntry<GravitronItem> =
        REGISTRATE.item<GravitronItem>("gravitron") { properties: Item.Properties? ->
            GravitronItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { GravitronItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val AURIC_CUBE: ItemEntry<AuricItem> =
        REGISTRATE.item<AuricItem>("auric_cube") { properties: Item.Properties? ->
            AuricItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { AuricCubeItemRenderer(false) } })
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()

    @JvmField
    val AURIC_MATRIX: ItemEntry<AuricItem> =
        REGISTRATE.item<AuricItem>("auric_matrix") { properties: Item.Properties? ->
            AuricItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { AuricCubeItemRenderer(true) } })
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()


    @JvmField
    val INCOMPLETE_AURIC_DESIGNATOR: ItemEntry<CWItem> =
        REGISTRATE.item<CWItem>("incomplete_auric_designator") { properties: Item.Properties? ->
            CWItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { AuricDesignatorItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val AURIC_DESIGNATOR: ItemEntry<AuricDesignatorItem> =
        REGISTRATE.item<AuricDesignatorItem>("auric_designator") { properties: Item.Properties? ->
            AuricDesignatorItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { AuricDesignatorItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val AURIC_CRYSTAL: ItemEntry<Item> = REGISTRATE.item<Item>("auric_crystal") { properties: Item.Properties? ->
        Item(properties!!)
    }
        .tab { ClockworkMod.BASE_CREATIVE_TAB }
        .register()


    @JvmStatic
    fun register() {

    }
}