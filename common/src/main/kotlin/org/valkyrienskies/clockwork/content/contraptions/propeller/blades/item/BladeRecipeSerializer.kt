package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import com.google.gson.JsonObject
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.GsonHelper
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.RecipeSerializer
import java.util.Objects


class BladeRecipeSerializer : RecipeSerializer<CraftingTableBladeRecipe> {
    override fun fromJson(id: ResourceLocation, json: JsonObject): CraftingTableBladeRecipe {
        val category: CraftingBookCategory = Objects.requireNonNullElse(
            CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null)), CraftingBookCategory.MISC
        )
        return CraftingTableBladeRecipe(id, category)
    }

    override fun fromNetwork(id: ResourceLocation, buf: FriendlyByteBuf): CraftingTableBladeRecipe {
        val category = buf.readEnum(CraftingBookCategory::class.java)
        return CraftingTableBladeRecipe(id, category)
    }



    override fun toNetwork(buf: FriendlyByteBuf, recipe: CraftingTableBladeRecipe) {
        buf.writeEnum(recipe.category())
    }
}