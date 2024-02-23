package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import com.simibubi.create.foundation.data.AssetLookup
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.curiosities.DebugWandItem
import org.valkyrienskies.clockwork.content.curiosities.WanderliteCubeItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.WanderliteItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItemRenderer
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
    val WANDERLITE_CUBE: ItemEntry<WanderliteItem> =
        REGISTRATE.item<WanderliteItem>("wanderlite_cube") { properties: Item.Properties? ->
            WanderliteItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderliteCubeItemRenderer(false) } })
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()

    @JvmField
    val WANDERLITE_MATRIX: ItemEntry<WanderliteItem> =
        REGISTRATE.item<WanderliteItem>("wanderlite_matrix") { properties: Item.Properties? ->
            WanderliteItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderliteCubeItemRenderer(true) } })
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()


    @JvmField
    val INCOMPLETE_WANDERWAND: ItemEntry<CWItem> =
        REGISTRATE.item<CWItem>("incomplete_wanderwand") { properties: Item.Properties? ->
            CWItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderWandItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val WANDERWAND: ItemEntry<WanderWandItem> =
        REGISTRATE.item<WanderWandItem>("wanderwand") { properties: Item.Properties? ->
            WanderWandItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderWandItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val WANDERLITE_CRYSTAL: ItemEntry<Item> =
        REGISTRATE.item<Item>("wanderlite_crystal") { properties: Item.Properties? ->
            Item(properties!!)
        }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()


    @JvmField
    val DEBUG_WAND: ItemEntry<DebugWandItem> =
        REGISTRATE.item<DebugWandItem>("debug_wand") { properties: Item.Properties? ->
            DebugWandItem(properties!!)
        }
            .tab { ClockworkMod.BASE_CREATIVE_TAB }
            .register()


    @JvmStatic
    fun register() {

    }
}