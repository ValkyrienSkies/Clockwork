package org.valkyrienskies.clockwork

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.registry.CreativeTabs
import org.valkyrienskies.clockwork.registry.DeferredRegister

@Suppress("unused")
object ClockWorkItems {
    private val ITEMS = DeferredRegister.create(ClockWorkMod.MOD_ID, Registry.ITEM_REGISTRY)
    val TAB: CreativeModeTab = CreativeTabs.create(
        ResourceLocation(
            ClockWorkMod.MOD_ID,
            "eureka_tab"
        )
    ) { ItemStack(ClockWorkBlocks.OAK_SHIP_HELM.get()) }

    fun register() {
        ClockWorkBlocks.registerItems(ITEMS)
        ITEMS.applyAll()
    }

    private infix fun Item.byName(name: String) = ITEMS.register(name) { this }
}
