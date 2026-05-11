package org.valkyrienskies.clockwork.platform

import dev.architectury.injectables.annotations.ExpectPlatform
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import net.minecraft.world.item.crafting.Recipe
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe

object GasCrafterJEI {
    /**
     * This function has to be split because of a SINGLE IMPORT for FluidStack.
     *
     * We return an [Int] because [i] the parameter is not mutable, so we _return_ the mutated [i] instead
     *
     * @param size the size of the slots
     * @param i the current index of the slots
     * @param recipe the [GasCraftingRecipe] to pull outputs from
     * @param builder the JEI recipe builder to use
     * @return the incremented [i]
     */
    @JvmStatic
    @ExpectPlatform
    fun addFluidOutputSlots(size: Int, i: Int, recipe: GasCraftingRecipe, builder: IRecipeLayoutBuilder): Int {
        throw AssertionError()
    }
}