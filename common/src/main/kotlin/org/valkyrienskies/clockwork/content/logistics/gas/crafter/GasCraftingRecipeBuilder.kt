package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import net.minecraft.core.NonNullList
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.integration.jei.KelvinGasIngredient

class GasCraftingRecipeBuilder {
    class GasRecipeParams: ProcessingRecipeBuilder.ProcessingRecipeParams(ClockworkMod.asResource("gas_crafting_recipe")) {
        var gasIngredients: NonNullList<KelvinGasIngredient>
        var gasResults: NonNullList<KelvinGasIngredient>

        init {
            gasIngredients = NonNullList.create<KelvinGasIngredient>()
            gasResults = NonNullList.create<KelvinGasIngredient>()

        }
    }

}