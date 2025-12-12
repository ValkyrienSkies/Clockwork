package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import com.simibubi.create.content.processing.recipe.ProcessingRecipeSerializer

class GasCraftingRecipeSerializer<T: GasCraftingRecipe>(factory: ProcessingRecipeBuilder.ProcessingRecipeFactory<T>) : ProcessingRecipeSerializer<T>(factory) {
}