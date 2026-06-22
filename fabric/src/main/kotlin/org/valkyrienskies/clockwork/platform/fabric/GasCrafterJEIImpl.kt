package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.compat.jei.category.CreateRecipeCategory.addFluidSlot
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe

object GasCrafterJEIImpl {
    @JvmStatic
    fun addFluidOutputSlots(size: Int, i: Int, recipe: GasCraftingRecipe, builder: IRecipeLayoutBuilder): Int {
        var i = i;
        // Do not be fooled into thinking this can be put in common
        // [fluidResult] compiles into a different FluidStack per loader
        for (fluidResult in recipe.getFluidResults()) {
            val xPosition = 142 - (if (size % 2 != 0 && i == size - 1) 0 else if (i % 2 == 0) 10 else -9)
            val yPosition = -19 * (i / 2) + 51
            addFluidSlot(builder, xPosition, yPosition, fluidResult)
            i++
        }
        return i
    }
}