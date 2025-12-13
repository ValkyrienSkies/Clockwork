package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import net.minecraft.core.NonNullList
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient

class GasCraftingRecipeBuilder(factory: ProcessingRecipeBuilder.ProcessingRecipeFactory<GasCraftingRecipe>, recipeId: ResourceLocation) {

    class GasRecipeParams: ProcessingRecipeBuilder.ProcessingRecipeParams(ClockworkMod.asResource("gas_crafting_recipe")) {
        var gasIngredients: NonNullList<KelvinGasIngredient> = NonNullList.create()
        var gasResults: NonNullList<KelvinGasIngredient> = NonNullList.create()
    }


}