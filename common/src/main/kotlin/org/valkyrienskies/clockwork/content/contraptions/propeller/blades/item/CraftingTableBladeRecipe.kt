package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import net.minecraft.core.RegistryAccess
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID

class CraftingTableBladeRecipe(id: ResourceLocation, category: CraftingBookCategory) : CustomRecipe(id, category) {

    override fun getType(): RecipeType<*>? {
        return BladeRecipeType
    }

    override fun matches(
        container: CraftingContainer,
        level: Level
    ): Boolean {
        println(container.items.toString())

        if (container.items.count() != 2) return false
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

        ClockworkMod.LOGGER.info("hello?")

        return newItem
    }

    override fun canCraftInDimensions(width: Int, height: Int): Boolean {
        return width*height >= 2
    }

    override fun getSerializer(): RecipeSerializer<*>? {
        return ClockworkMod.BLADE_SERIALIZER.get()
    }

    object BladeRecipeType: RecipeType<CraftingTableBladeRecipe> {
        override fun toString(): String {
            return "$MOD_ID:blade_crafting_recipe"
        }
    }
}