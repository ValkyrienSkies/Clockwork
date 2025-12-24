package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkRecipes

class CraftingTableBladeRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {

    override fun matches(
        container: CraftingContainer,
        level: Level
    ): Boolean {

        var isWide = false
        var numBlades = 0
        for (item in container.items) {
            if (item.isEmpty) continue
            if (item.item !is BladeItem) return false

            if (item.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get()) && !isWide) {
                if (numBlades > 0) return false
                isWide = true
            } else if (item.`is`(ClockworkItems.PROPELLER_BLADE.get()) && isWide) return  false

            numBlades++
        }
        return numBlades >= 2
    }

    override fun assemble(
        container: CraftingContainer,
        registryAccess: RegistryAccess
    ): ItemStack? {
        var length = 0.0

        val newItem = ItemStack(container.items.first().item)

        if (container.containerSize == 0) return ItemStack.EMPTY

        for (i in 0..container.containerSize) {
            val item = container.getItem(i)
            length += item.tag?.getDouble("BladeLength") ?: 0.0
        }

        newItem.orCreateTag.putDouble("BladeLength", length)


        return newItem
    }



    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        //println(width)
        return width*height >= 2
    }

    override fun getSerializer(): RecipeSerializer<*>? {
        return ClockworkRecipes.BLADE_CRAFTING_SERIALIZER.get()

    }
}
