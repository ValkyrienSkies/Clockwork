package org.valkyrienskies.clockwork.compat.jei.categories

import com.simibubi.create.compat.jei.ItemIcon
import com.simibubi.create.compat.jei.category.CreateRecipeCategory.getRenderedSlot
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IJeiHelpers
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI.Companion.addOutputGasSlot
import org.valkyrienskies.clockwork.compat.jei.DuctStatsDummyRecipe
import org.valkyrienskies.clockwork.compat.jei.DuctStatsDummyRecipe.Companion.DUCT_STATS_DUMMY_TYPE
import org.valkyrienskies.clockwork.compat.jei.animated_blocks.AnimatedDuct
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
class DuctStatsCategory(val helpers: IJeiHelpers) : IRecipeCategory<DuctStatsDummyRecipe> {
    private val guiHelper = helpers.guiHelper
    private var currentRecipe: DuctStatsDummyRecipe? = null

    override fun getRecipeType(): RecipeType<DuctStatsDummyRecipe> {
        return DUCT_STATS_DUMMY_TYPE
    }

    override fun getTitle(): Component {

        return Component.literal("Gas Production")
    }

    override fun getIcon(): IDrawable {
        return ItemIcon { ItemStack(ClockworkBlocks.STEAM_GENERATOR.asItem()) }
    }

    override fun getWidth(): Int = 177

    override fun getHeight(): Int = 40


    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: DuctStatsDummyRecipe, focuses: IFocusGroup) {
        currentRecipe = recipe

        builder
            .addSlot(RecipeIngredientRole.INPUT, 25, 0)
            .setBackground(getRenderedSlot(), -1, -1)
            .addItemStacks(mutableListOf(ItemStack(recipe.block.asItem())))

        addOutputGasSlot(builder, width-25, 0, KelvinGasIngredient(GasTypeRegistry.getGasType(recipe.info.first)!!, 0.0))
    }

    override fun draw(
        recipe: DuctStatsDummyRecipe,
        iRecipeSlotsView: IRecipeSlotsView,
        graphics: GuiGraphics,
        mouseX: Double,
        mouseY: Double
    ) {
        guiHelper.recipeArrowFilled.draw(graphics, width / 2 - (guiHelper.recipeArrowFilled.width / 2), 0)

        val info = recipe.info.second
        val methodComponent = Component.translatable(info.method.langKey)
        val typeComponent = Component.translatable(info.type.langKey)
        var fullComponent = Component.empty().append(methodComponent).append(" (").append(typeComponent)
        if (info.condition.toString().isNotEmpty()) {
            fullComponent = fullComponent.append(info.condition)
        }
        fullComponent = fullComponent.append(")")

        ClockworkGuiTextures.JEI_DARKER_BAR_FULL.render(graphics, 4, 0+20)

        val scale = 0.75f
        graphics.pose().pushPose();
        // We do x and y here so it auto-scales
        graphics.pose().translate(10f, 26f, 0f)
        graphics.pose().scale(scale, scale, 1f);
        graphics.drawString(Minecraft.getInstance().font, fullComponent.withStyle(ChatFormatting.WHITE), 0, 0, 16777215)
        graphics.pose().popPose();
    }

}