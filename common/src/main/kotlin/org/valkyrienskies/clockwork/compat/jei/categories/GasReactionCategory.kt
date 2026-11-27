package org.valkyrienskies.clockwork.compat.jei.categories

import com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot
import com.simibubi.create.foundation.gui.AllGuiTextures
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.builder.ITooltipBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addInputGasSlot
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addOutputGasSlot
import org.valkyrienskies.clockwork.compat.jei.GasThermalBar
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import org.valkyrienskies.kelvin.integration.jei.ImageDrawable
import org.valkyrienskies.kelvin.integration.jei.KelvinJeiPlugin
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class GasReactionCategory(val guiHelper: IGuiHelper) : IRecipeCategory<GasBaseRecipe> {
    private var currentRecipe: GasBaseRecipe? = null

    override fun getRecipeType(): RecipeType<GasBaseRecipe> {
        return KelvinJeiPlugin.GAS_REACTION_RECIPE_TYPE
    }

    override fun getTitle(): Component {
        return Component.literal("Gas Reaction")
    }

    override fun getIcon(): IDrawable {
        return ImageDrawable(16, 16, GasType.PLACEHOLDER_ICON)
    }

    override fun getWidth(): Int = 177

    private fun topSectionHeight(recipe: GasBaseRecipe?): Int {
        return if (recipe != null && GasThermalBar.fromGasReaction(recipe) != null) 34 else 20
    }

    override fun getHeight(): Int {
        val recipe = currentRecipe ?: return 20

        val requirementCount = recipe.requirements.size
        return topSectionHeight(recipe) + (requirementCount * 20)
    }

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: GasBaseRecipe, focuses: IFocusGroup) {
        currentRecipe = recipe
        val slotYOffset = if (GasThermalBar.fromGasReaction(recipe) != null) 7 else 0

        var size = recipe.gasses.size
        var xOffset = if (size < 3) (3 - size) * 19 / 2 else 0

        var i = 0
        for (gasIngredient in recipe.gasses) {
            val x = xOffset + (i % 3) * 19
            val y = slotYOffset + (i / 3) * 19
            addInputGasSlot(builder, x, y, KelvinGasIngredient(gasIngredient.key, gasIngredient.value), getRenderedSlot())
            i++
        }

        size = recipe.result.size
        xOffset = if (size < 2) (2 - size) * 19 / 2 else 0
        i = 0

        for (gasIngredient in recipe.result) {
            // Why 25? No idea, but it's the magic number which made the gap
            // from the border for the outputs the exact same gap as the inputs
            val x = width - (xOffset + (i % 2) * 19) - 25
            val y = slotYOffset + (i / 3) * 19
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
        val thermal = GasThermalBar.fromGasReaction(recipe)
        val arrowY = if (thermal != null) 1 else 0
        guiHelper.recipeArrowFilled.draw(graphics, width / 2 - (guiHelper.recipeArrowFilled.width / 2), arrowY)

        if (thermal != null) {
            GasThermalBar.render(graphics, width / 2, 20, thermal)
        }

        val reqStartY = topSectionHeight(recipe)
        var i = 0
        recipe.requirements.forEach {
            ClockworkGuiTextures.JEI_DARKER_BAR_FULL.render(graphics, 4, reqStartY + 20 * i)
            graphics.drawString(Minecraft.getInstance().font, it.key.get_text(it.value), 7, reqStartY + 5 + 20 * i, 16777215)
            i++
        }
    }

    override fun getTooltip(
        tooltip: ITooltipBuilder,
        recipe: GasBaseRecipe,
        recipeSlotsView: IRecipeSlotsView,
        mouseX: Double,
        mouseY: Double
    ) {
        val thermal = GasThermalBar.fromGasReaction(recipe) ?: return
        val bounds = GasThermalBar.boundsCentered(width / 2, 20, thermal)
        if (bounds.contains(mouseX, mouseY)) {
            GasThermalBar.addTooltip(tooltip, thermal)
        }
    }
}