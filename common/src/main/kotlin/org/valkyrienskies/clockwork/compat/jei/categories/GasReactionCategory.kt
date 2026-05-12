package org.valkyrienskies.clockwork.compat.jei.categories

import com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot
import com.simibubi.create.foundation.gui.AllGuiTextures
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addInputGasSlot
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addOutputGasSlot
import org.valkyrienskies.clockwork.compat.jei.animated_blocks.AnimatedDuct
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import org.valkyrienskies.kelvin.integration.jei.ImageDrawable
import org.valkyrienskies.kelvin.integration.jei.KelvinJeiPlugin
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class GasReactionCategory : IRecipeCategory<GasBaseRecipe> {
    private val duct = AnimatedDuct()
    private var currentRecipe: GasBaseRecipe? = null

    override fun getRecipeType(): RecipeType<GasBaseRecipe> {
        return KelvinJeiPlugin.GAS_REACTION_RECIPE_TYPE
    }

    override fun getTitle(): Component {

        return Component.literal("Gas Reaction")
    }

    override fun getIcon(): IDrawable {
        return ImageDrawable(16,16, GasType.PLACEHOLDER_ICON)
    }

    override fun getWidth(): Int = 177

    override fun getHeight(): Int {
        val recipe = currentRecipe ?: return 83

        val requirementCount = recipe.requirements.size
        val calculatedHeight = 83 + (requirementCount * 20)
        return calculatedHeight
    }


    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: GasBaseRecipe, focuses: IFocusGroup) {
        currentRecipe = recipe


        var size = recipe.gasses.size
        val xOffset = if (size < 3) (3 - size) * 19 / 2 else 0

        var i = 0
        for (gasIngredient in recipe.gasses) {
            val x = 17 + xOffset + (i % 3) * 19
            val y = 51 - (i / 3) * 19
            addInputGasSlot(builder, x, y, KelvinGasIngredient(gasIngredient.key, gasIngredient.value), getRenderedSlot())
            i++
        }

        size = recipe.result.size
        i = 0

        for (gasIngredient in recipe.result) {
            val x = 142 - (if (size % 2 != 0 && i == size - 1) 0 else if (i % 2 == 0) 10 else -9)
            val y = 51 - (i / 2) * 19
            addOutputGasSlot(builder, x, y, KelvinGasIngredient(gasIngredient.key, gasIngredient.value), getRenderedSlot())
            i++
        }
    }

    override fun draw(
        recipe: GasBaseRecipe,
        iRecipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val vRows = (1 + (recipe.result.size)) / 2

        if (vRows <= 2) AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32)

        val shadow = AllGuiTextures.JEI_SHADOW
        shadow.render(graphics, 81, 68)

        duct.draw(graphics, width / 2 + 3, 34)

        var i = 0
        recipe.requirements.forEach {
            ClockworkGuiTextures.JEI_DARKER_BAR.render(graphics, 4, 80+20*i)
            graphics.drawString(Minecraft.getInstance().font, it.key.get_text(it.value), 7, 85+20*i, 16777215)
            i++
        }
    }

}