package org.valkyrienskies.clockwork

import com.simibubi.create.AllTags
import com.simibubi.create.foundation.data.AssetLookup
import com.tterrag.registrate.util.entry.ItemEntry
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItem
import org.valkyrienskies.clockwork.content.curiosities.tools.designator.AuricDesignatorItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer
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
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { GravitronItemRenderer() } })
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
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { AuricDesignatorItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    private fun ingredient(name: String): ItemEntry<Item> {
        return REGISTRATE.item<Item>(name) { properties: Item.Properties? -> Item(properties!!) }.register()
    }

    @JvmStatic
    fun register() {
    }
}