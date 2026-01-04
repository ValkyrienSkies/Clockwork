package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import com.simibubi.create.content.kinetics.saw.CuttingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import net.minecraft.util.Mth
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkRecipes
import kotlin.math.max

class CuttingBladeRecipe(params: ProcessingRecipeBuilder.ProcessingRecipeParams) : CuttingRecipe(params) {
    override fun getType(): RecipeType<*> {
        return ClockworkRecipes.ClockworkRecipeTypes.BLADE_CUTTING.getType()
    }

    companion object {
        fun cutBlade(blade: ItemStack): ItemStack {
            val new = blade.copy()

            val length = new.orCreateTag.getDouble("BladeLength")
            new.tag!!.putDouble("BladeLength", max(length/2, 0.25))

            return new
        }
    }
}