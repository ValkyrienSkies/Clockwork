package org.valkyrienskies.clockwork

import com.simibubi.create.AllItems
import com.simibubi.create.AllTags
import com.simibubi.create.AllTags.AllItemTags
import com.simibubi.create.foundation.data.AssetLookup
import com.tterrag.registrate.util.entry.ItemEntry
import dev.architectury.core.item.ArchitecturyRecordItem
import net.fabricmc.fabric.api.item.v1.FabricItemSettings
import net.minecraft.tags.ItemTags
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.RecordItem
import net.minecraft.world.item.SwordItem
import net.minecraft.world.item.Tiers
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeItem
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.WanderliteCubeItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.WanderliteItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.CreativeGravitronItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItem
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronItemRenderer
import org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver.ScrewdriverItem
import org.valkyrienskies.clockwork.content.kinetics.universal_shaft.UniversalShaftItem
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBackTankItem
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonHoseItem
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.clockwork.util.builder.ClockworkRegistrate
import java.util.function.Supplier

object ClockworkItems {

    @JvmField
    val WANDERLUST_DISC: ItemEntry<RecordItem> =
        REGISTRATE.item<RecordItem>("music_disc_wanderlust") { properties: Item.Properties? ->
            RecordItem(7, ClockworkSounds.WANDERLUST.mainEvent!!, properties!!, 84)
        }
            .properties { it.rarity(Rarity.EPIC) }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .tag(ItemTags.MUSIC_DISCS)
            .register()

    @JvmField
    val GRAVITRON: ItemEntry<GravitronItem> =
        REGISTRATE.item<GravitronItem>("gravitron") { properties: Item.Properties? ->
            GravitronItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { GravitronItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val CREATIVE_GRAVITRON: ItemEntry<CreativeGravitronItem> =
        REGISTRATE.item<CreativeGravitronItem>("creative_gravitron") { properties: Item.Properties? ->
            CreativeGravitronItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
                it.rarity(Rarity.UNCOMMON)
            }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .register()

    @JvmField
    val WANDERLITE_CUBE: ItemEntry<WanderliteItem> =
        REGISTRATE.item<WanderliteItem>("wanderlite_cube") { properties: Item.Properties? ->
            WanderliteItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderliteCubeItemRenderer(false) } })
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .register()

    @JvmField
    val WANDERLITE_MATRIX: ItemEntry<WanderliteItem> =
        REGISTRATE.item<WanderliteItem>("wanderlite_matrix") { properties: Item.Properties? ->
            WanderliteItem(properties!!)
        }
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderliteCubeItemRenderer(true) } })
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
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
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderWandItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()


    @JvmField
    val PROPELLER_BLADE: ItemEntry<BladeItem> = REGISTRATE.item("propeller_blade") { properties: Item.Properties? ->
        BladeItem(Tiers.WOOD, 2, 0.4f, properties!!)
    }
        .properties {
            it.durability(100)
        }
        .transform(ClockworkRegistrate.customRenderedItem { Supplier { BladeItemRenderer() } })
        .tag(ClockworkTags.AllItemTags.PROP_BLADE.tag)
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .register()

    @JvmField
    val WIDE_PROPELLER_BLADE: ItemEntry<BladeItem> = REGISTRATE.item("wide_propeller_blade") { properties: Item.Properties? ->
        BladeItem(Tiers.WOOD, 4, 0.2f, properties!!)
    }
        .properties {
            it.durability(200)
        }
        .transform(ClockworkRegistrate.customRenderedItem { Supplier { BladeItemRenderer() } })
        .tag(ClockworkTags.AllItemTags.PROP_BLADE.tag)
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
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
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .transform(ClockworkRegistrate.customRenderedItem { Supplier { WanderWandItemRenderer() } })
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.itemModelWithPartials())
            .register()

    @JvmField
    val WANDERLITE_CRYSTAL: ItemEntry<Item> = REGISTRATE.item<Item>("wanderlite_crystal") { properties: Item.Properties? ->
        Item(properties!!)
    }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .register()

    @JvmField
    val SCREWDRIVER: ItemEntry<ScrewdriverItem> =
        REGISTRATE.item<ScrewdriverItem>("screwdriver") { properties: Item.Properties? ->
            ScrewdriverItem(properties!!)
        }
            .properties {
                it.stacksTo(1)
            }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .tag(AllTags.AllItemTags.WRENCH.tag)
            .model(AssetLookup.existingItemModel())
            .register()

    @JvmField
    val GAS_BANKTANK: ItemEntry<GasBackTankItem> = REGISTRATE.item<GasBackTankItem>("gas_backtank") { properties: Item.Properties? ->
        GasBackTankItem(ClockworkBlocks.GAS_BACKTANK.get(), properties!!)
    }
        .properties { properties: Item.Properties ->
            properties.stacksTo(1)
        }
        .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
        .tag(AllItemTags.PRESSURIZED_AIR_SOURCES.tag)
        .register()

    @JvmField
    val UNIVERSAL_SHAFT_ITEM: ItemEntry<UniversalShaftItem> =
        REGISTRATE.item<UniversalShaftItem>("universal_shaft_item") { properties: Item.Properties? ->
            UniversalShaftItem(properties!!)
        }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .register()

    @JvmField
    val EXTENDON_HOSE: ItemEntry<ExtendonHoseItem> =
        REGISTRATE.item<ExtendonHoseItem>("extendon_hose") { properties: Item.Properties? ->
            ExtendonHoseItem(properties!!)
        }
            .tab(ClockworkMod.BASE_CREATIVE_TABINFO)
            .register()

    @JvmStatic
    fun register() {

    }
}
