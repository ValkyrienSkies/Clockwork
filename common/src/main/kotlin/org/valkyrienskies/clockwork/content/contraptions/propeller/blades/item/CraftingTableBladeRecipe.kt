package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkRecipes

class CraftingTableBladeRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {

    override fun matches(
        container: CraftingContainer,
        level: Level
    ): Boolean {

        var isWide = false
        var numBlades = 0
        var length = 0.0

        for (item in container.items) {
            if (item.isEmpty) continue
            if (item.item !is BladeItem) return false

            if (item.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get()) && !isWide) {
                if (numBlades > 0) return false
                isWide = true
            } else if (item.`is`(ClockworkItems.PROPELLER_BLADE.get()) && isWide) return  false
            length += item.tag?.getDouble("BladeLength") ?: 0.0

            numBlades++
        }
        return numBlades >= 2 && length <= ClockworkConfig.SERVER.maxBladeSize
    }

    override fun assemble(
        container: CraftingContainer,
        registryAccess: RegistryAccess
    ): ItemStack? {
        val items = container.items.filter { i -> i.item is BladeItem }
        if (items.size < 2) return ItemStack.EMPTY

        val newItem = ItemStack(items.first().item)
        val length = items.map { i -> i.tag?.getDouble("BladeLength") ?: 0.0 }
            .fold(0.0) { acc, d -> acc + d }

        return newItem.apply { orCreateTag.putDouble("BladeLength", length) }
    }



    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width*height >= 2
    }

    override fun getSerializer(): RecipeSerializer<*>? {
        return ClockworkRecipes.BLADE_CRAFTING_SERIALIZER.get()
    }
}
