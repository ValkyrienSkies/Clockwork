package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.AllRecipeTypes
import com.simibubi.create.content.processing.recipe.ProcessingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder.ProcessingRecipeParams
import com.simibubi.create.foundation.fluid.FluidIngredient
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo
import dev.architectury.injectables.annotations.ExpectPlatform
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack
import net.minecraft.core.NonNullList
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.Level
import org.valkyrienskies.kelvin.integration.jei.GasIngredientType
import org.valkyrienskies.kelvin.integration.jei.KelvinGasIngredient
import kotlin.jvm.optionals.getOrNull
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipeBuilder.GasRecipeParams

open class GasCraftingRecipe(type: IRecipeTypeInfo, params: GasRecipeParams) :
    ProcessingRecipe<Container>(type, params) {

    protected var gasIngredients: NonNullList<KelvinGasIngredient> = params.gasIngredients
    protected var gasResults: NonNullList<KelvinGasIngredient> = params.gasResults

    constructor(params: GasRecipeParams) : this(AllRecipeTypes.BASIN, params)

    companion object {
        @JvmStatic
        fun match(be: GasCrafterBlockEntity, recipe: Recipe<*>): Boolean {

            val basin = be.getBasin().getOrNull() ?: return false
            val filter = basin.filter ?: return false

            var filterTest = filter.test(recipe.getResultItem(basin.level!!.registryAccess()))

            if (recipe is GasCraftingRecipe) {
                if (recipe.rollableResults.isEmpty() && !recipe.fluidResults.isEmpty()) {
                    filterTest = filter.test(recipe.fluidResults[0])
                }
            }

            if (!filterTest) return false

            return apply(be, recipe, true)
        }

        @JvmStatic
        fun apply(basin: GasCrafterBlockEntity, recipe: Recipe<*>): Boolean {
            return apply(basin, recipe, false)
        }

        @ExpectPlatform
        private fun apply(be: GasCrafterBlockEntity, recipe: Recipe<*>, test: Boolean): Boolean {
            throw AssertionError()
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
}