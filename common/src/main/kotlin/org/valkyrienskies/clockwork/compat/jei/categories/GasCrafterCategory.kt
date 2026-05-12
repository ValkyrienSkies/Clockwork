package org.valkyrienskies.clockwork.compat.jei.categories

import com.simibubi.create.AllBlocks
import com.simibubi.create.AllItems
import com.simibubi.create.compat.jei.category.CreateRecipeCategory
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
import com.simibubi.create.content.processing.recipe.HeatCondition
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.item.ItemHelper
import com.simibubi.create.foundation.utility.CreateLang
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addInputGasSlot
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addOutputGasSlot
import org.valkyrienskies.clockwork.compat.jei.animated_blocks.AnimatedGasCrafter
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import org.valkyrienskies.clockwork.platform.GasCrafterJEI.addFluidOutputSlots
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class GasCrafterCategory(val info: Info<GasCraftingRecipe>, val guiHelper: IGuiHelper) : CreateRecipeCategory<GasCraftingRecipe>(info) {
    private val crafter = AnimatedGasCrafter()
    private val heater = AnimatedBlazeBurner()
    private var currentRecipe: GasCraftingRecipe? = null

    override fun draw(
        recipe: GasCraftingRecipe,
        iRecipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        val requiredHeat = recipe.getRequiredHeat()

        val noHeat = requiredHeat == HeatCondition.NONE

        val vRows = (1 + recipe.getFluidResults().size + recipe.getRollableResults().size + (recipe.gasRecipe?.result?.size ?: 0)) / 2

        if (vRows <= 2) AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 136, -19 * (vRows - 1) + 32)

        val shadow = if (noHeat) AllGuiTextures.JEI_SHADOW else AllGuiTextures.JEI_LIGHT
        shadow.render(graphics, 81, 58 + (if (noHeat) 10 else 30))

        if (!noHeat) {
            AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, 80)
            graphics.drawString(
                Minecraft.getInstance().font, CreateLang.translateDirect(requiredHeat.getTranslationKey()), 9,
                86, requiredHeat.getColor(), false
            )
        }


        if (requiredHeat != HeatCondition.NONE) heater.withHeat(requiredHeat.visualizeAsBlazeBurner())
            .draw(graphics, width / 2 + 3, 55)
        crafter.draw(graphics, width / 2 + 3, 34)


        var i = if (requiredHeat == HeatCondition.NONE) 0 else 1

        if (recipe.gasRecipe?.requirements != null)
        recipe.gasRecipe!!.requirements.forEach {
            ClockworkGuiTextures.JEI_DARKER_BAR.render(graphics, 4, 80+20*i)
            graphics.drawString(Minecraft.getInstance().font, it.key.get_text(it.value), 9, 86+20*i, 16777215, false)
            i++
        }
    }

    override fun getWidth(): Int = 177

    override fun getHeight(): Int {
        val recipe = currentRecipe ?: return 83

        val requirementCount = (if (recipe.requiredHeat != HeatCondition.NONE) 1 else 0) + (recipe.gasRecipe?.requirements?.size ?: return 83)
        val calculatedHeight = 83 + (requirementCount * 20)
        return calculatedHeight
    }

    override fun setRecipe(
        builder: IRecipeLayoutBuilder,
        recipe: GasCraftingRecipe,
        focuses: IFocusGroup
    ) {
        this.currentRecipe = recipe

        val condensedIngredients = ItemHelper.condenseIngredients(recipe.getIngredients())

        var size = condensedIngredients.size + recipe.getFluidIngredients().size + (recipe.gasRecipe?.gasses?.size ?: 0)
        val xOffset = if (size < 3) (3 - size) * 19 / 2 else 0
        var i = 0

        for (pair in condensedIngredients) {
            val stacks: MutableList<ItemStack?> = ArrayList<ItemStack?>()
            for (itemStack in pair.getFirst()!!.getItems()) {
                val copy = itemStack.copy()
                copy.setCount(pair.getSecond()!!.getValue())
                stacks.add(copy)
            }

            builder
                .addSlot(RecipeIngredientRole.INPUT, 17 + xOffset + (i % 3) * 19, 51 - (i / 3) * 19)
                .setBackground(getRenderedSlot(), -1, -1)
                .addItemStacks(stacks)
            i++
        }
        for (fluidIngredient in recipe.getFluidIngredients()) {
            val x = 17 + xOffset + (i % 3) * 19
            val y = 51 - (i / 3) * 19
            addFluidSlot(builder, x, y, fluidIngredient)
            i++
        }

        if (recipe.gasRecipe != null)
        for (gasIngredient in recipe.gasRecipe!!.gasses) {
            val x = 17 + xOffset + (i % 3) * 19
            val y = 51 - (i / 3) * 19
            addInputGasSlot(builder, x, y, KelvinGasIngredient(gasIngredient.key, gasIngredient.value), getRenderedSlot())
            i++
        }

        size = recipe.getRollableResults().size + recipe.getFluidResults().size + (recipe.gasRecipe?.result?.size ?: 0)
        i = 0

        for (result in recipe.getRollableResults()) {
            val xPosition = 142 - (if (size % 2 != 0 && i == size - 1) 0 else if (i % 2 == 0) 10 else -9)
            val yPosition = -19 * (i / 2) + 51

            builder
                .addSlot(RecipeIngredientRole.OUTPUT, xPosition, yPosition)
                .setBackground(getRenderedSlot(result), -1, -1)
                .addItemStack(result.getStack())
                .addRichTooltipCallback(addStochasticTooltip(result))
            i++
        }

        i = addFluidOutputSlots(size, i, recipe, builder)

        if (recipe.gasRecipe != null)
            for (gasIngredient in recipe.gasRecipe!!.result) {
                val x = 142 - (if (size % 2 != 0 && i == size - 1) 0 else if (i % 2 == 0) 10 else -9)
                val y = -19 * (i / 2) + 51
                addOutputGasSlot(builder, x, y, KelvinGasIngredient(gasIngredient.key, gasIngredient.value), getRenderedSlot())
                i++
            }

        val requiredHeat = recipe.getRequiredHeat()
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.NONE)) {
            builder
                .addSlot(RecipeIngredientRole.RENDER_ONLY, 134, 81)
                .addItemStack(AllBlocks.BLAZE_BURNER.asStack())
        }
        if (!requiredHeat.testBlazeBurner(BlazeBurnerBlock.HeatLevel.KINDLED)) {
            builder
                .addSlot(RecipeIngredientRole.CATALYST, 153, 81)
                .addItemStack(AllItems.BLAZE_CAKE.asStack())
        }
    }
}