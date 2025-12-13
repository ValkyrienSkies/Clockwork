package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.google.gson.JsonObject
import com.simibubi.create.AllRecipeTypes
import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.NonNullList
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkRecipes
import kotlin.jvm.optionals.getOrNull
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipeBuilder.GasRecipeParams
import org.valkyrienskies.clockwork.platform.GasCrafterMethods
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import org.valkyrienskies.kelvin.impl.recipe.KelvinGasRecipeSerializer

open class GasCraftingRecipe(type: IRecipeTypeInfo, params: ProcessingRecipeBuilder.ProcessingRecipeParams) :
    ProcessingRecipe<Container>(type, params) {

    var gasIngredients: NonNullList<KelvinGasIngredient> = NonNullList.create()
    var gasResults: NonNullList<KelvinGasIngredient> = NonNullList.create()

    constructor(params: GasRecipeParams) : this(ClockworkRecipes.ClockworkRecipeTypes.GAS_CRAFTING, params)
    constructor(params: ProcessingRecipeBuilder.ProcessingRecipeParams) : this(ClockworkRecipes.ClockworkRecipeTypes.GAS_CRAFTING, params)

    companion object {
        @JvmStatic
        fun match(be: GasCrafterBlockEntity, recipe: Recipe<*>): Boolean {
            val basin = be.getBasin().getOrNull() ?: return false
            return apply(be, recipe, true)
        }

        @JvmStatic
        fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>): Boolean {
            return apply(be, recipe, false)
        }

        @JvmStatic
        fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
            return GasCrafterMethods.apply(be, recipe, test)
        }


    }



    override fun getMaxInputCount(): Int {
        return 64
    }

    override fun getMaxOutputCount(): Int {
        return 4
    }

    override fun getMaxFluidInputCount(): Int {
        return 2
    }

    override fun getMaxFluidOutputCount(): Int {
        return 2
    }

    override fun canRequireHeat(): Boolean {
        return true
    }

    override fun canSpecifyDuration(): Boolean {
        return true
    }

    override fun matches(inv: Container, worldIn: Level): Boolean {
        return false
    }

    override fun readAdditional(json: JsonObject) {
        val gasBaseRecipe = KelvinGasRecipeSerializer.parse(json) ?: return

        gasBaseRecipe.gasses.forEach { gasIngredients.add(KelvinGasIngredient(it.key, it.value)) }
        gasBaseRecipe.result.forEach { gasResults.add(KelvinGasIngredient(it.key, it.value)) }

    }

    override fun writeAdditional(json: JsonObject) {

    }
}