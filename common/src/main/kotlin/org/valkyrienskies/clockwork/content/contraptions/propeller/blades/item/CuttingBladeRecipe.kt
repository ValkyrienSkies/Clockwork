package org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item

import com.simibubi.create.content.kinetics.saw.CuttingRecipe
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder
import net.minecraft.core.RegistryAccess
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class CuttingBladeRecipe(params: ProcessingRecipeBuilder.ProcessingRecipeParams) : CuttingRecipe(params) {
    override fun matches(inv: Container, worldIn: Level): Boolean {
       return inv.getItem(0).item is BladeItem
    }

    override fun assemble(inv: Container, registryAccess: RegistryAccess): ItemStack? {
        val blade = inv.getItem(0)
        val new = blade.copy()

        val length = new.orCreateTag.getDouble("BladeLength")
        new.tag!!.putDouble("BladeLength", length/2)

        return new
    }

    override fun getResultItem(registryAccess: RegistryAccess): ItemStack {
        return ItemStack.EMPTY
    }
}