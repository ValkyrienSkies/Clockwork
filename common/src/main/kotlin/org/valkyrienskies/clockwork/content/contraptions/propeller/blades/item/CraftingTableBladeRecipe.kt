package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import net.minecraft.core.NonNullList
import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingRecipe
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkRecipes

class CraftingTableBladeRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {

    init {
        println(id)
        println(category)
    }

    override fun matches(
        container: CraftingContainer,
        level: Level
    ): Boolean {

        val amount = container.items.sumBy { if (it.isEmpty) 0 else 1 }

        if (amount != 2) return false
        if (container.items.first().item !is BladeItem) return false
        if (!ItemStack.isSameItem(container.items[0],container.items[1])) return false

        return true
    }

    override fun assemble(
        container: CraftingContainer,
        registryAccess: RegistryAccess
    ): ItemStack? {
        var length = 0.0

        println(container.items.first())
        container.items.forEach { length += it.tag?.getDouble("BladeLength") ?: 0.0
            println(it.tag?.getDouble("BladeLength"))
            println(length)
        }

        val newItem = container.items.first().copy()
        newItem.tag!!.putDouble("BladeLength", length)


        return newItem
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        println(width)
        return width*height >= 2
    }

    override fun getSerializer(): RecipeSerializer<*>? {
        return ClockworkRecipes.BLADE_RECIPE_SERIALIZER.get()

    }
//    override fun getType(): RecipeType<*>? {
//        return ClockworkRecipes.BLADE_RECIPE_TYPE.get()
//    }

    override fun getIngredients(): NonNullList<Ingredient?>? {
        return super.getIngredients()
    }

}