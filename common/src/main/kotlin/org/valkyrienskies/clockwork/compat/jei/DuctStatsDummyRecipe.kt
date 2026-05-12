package org.valkyrienskies.clockwork.compat.jei

import mezz.jei.api.recipe.RecipeType
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.gui.ProductionInfo

class DuctStatsDummyRecipe(val block: Block, val info: Pair<ResourceLocation, ProductionInfo>) {
    companion object {
        val DUCT_STATS_DUMMY_TYPE = RecipeType.create(
            ClockworkMod.MOD_ID,
            "duct_stats_dummy",
            DuctStatsDummyRecipe::class.java
        )
    }
}