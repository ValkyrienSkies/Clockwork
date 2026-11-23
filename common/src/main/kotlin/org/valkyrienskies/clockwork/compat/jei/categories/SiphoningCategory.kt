package org.valkyrienskies.clockwork.compat.jei.categories

import com.mojang.math.Axis
import com.simibubi.create.compat.jei.ItemIcon
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.ingredient.IRecipeSlotsView
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.recipe.category.IRecipeCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.SpawnEggItem
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.compat.jei.ClockworkJEI
import org.valkyrienskies.clockwork.content.curiosities.harpoon.SiphoningRecipe
import org.valkyrienskies.kelvin.api.recipe.KelvinGasIngredient

class SiphoningCategory(private val helper: IGuiHelper) : IRecipeCategory<SiphoningRecipe> {
    private val preview = SiphoningPreview()

    override fun getRecipeType() = TYPE
    override fun getTitle() = Component.translatable("vs_clockwork.recipe.siphoning")
    override fun getIcon() = ItemIcon { ClockworkBlocks.SIPHON_HARPOON.asStack() }
    override fun getWidth() = 176
    override fun getHeight() = 118

    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: SiphoningRecipe, focuses: IFocusGroup) {
        SpawnEggItem.byId(recipe.entityType)?.let { egg ->
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStack(egg.defaultInstance)
        }
        ClockworkJEI.addOutputGasSlot(builder, 20, 39, KelvinGasIngredient(recipe.gas, recipe.massPerTick(64.0)))
    }

    override fun draw(recipe: SiphoningRecipe, slots: IRecipeSlotsView, graphics: GuiGraphics, mouseX: Double, mouseY: Double) {
        val font = Minecraft.getInstance().font
        preview.draw(graphics, recipe.entityType, 56, 0)
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(57f, 52f, 0f)
        pose.mulPose(Axis.ZP.rotationDegrees(180f))
        pose.scale(.7f, .7f, 1f)
        helper.recipeArrowFilled.draw(graphics, 0, 0)
        pose.popPose()
        fun label(text: Component, center: Int, maxWidth: Int) {
            val line = font.substrByWidth(text, maxWidth)
            graphics.drawString(font, line.string, center - font.width(line) / 2, 65, 0x404040, false)
        }
        label(Component.literal(recipe.gas.name), 28, 56)
        label(recipe.entityType.description, 124, 101)
        graphics.hLine(4, width - 5, 77, 0xffb8b8b8.toInt())
        val config = ClockworkConfig.SERVER.siphonHarpoon
        fun row(key: String, value: Double, y: Int) {
            graphics.drawString(font, ClockworkLang.translateDirect("recipe.siphoning.$key", ClockworkLang.number(value)),
                5, y, 0x404040, false)
        }
        row("yield", recipe.massPerTick(64.0) * 1000.0, 83)
        row("safe_speed", config.safeRpm, 95)
        row("damage", config.damagePerExcessRpm, 107)
    }

    companion object {
        val TYPE: RecipeType<SiphoningRecipe> = RecipeType.create(ClockworkMod.MOD_ID, "siphoning", SiphoningRecipe::class.java)
    }
}
