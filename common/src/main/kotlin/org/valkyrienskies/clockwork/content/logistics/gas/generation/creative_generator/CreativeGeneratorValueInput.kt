package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

class CreativeGeneratorValueInput(x: Int, y: Int, private val limit: Int, private val unit: String) : ScrollInput(x, y, 52, 18) {
    init {
        withRange(0, limit + 1)
        withStepFunction { if (it.control) 1000 else if (it.shift) 100 else 10 }
        withShiftStep(1)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        super.mouseScrolled(mouseX, mouseY, delta)
        return true
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        graphics.drawCenteredString(Minecraft.getInstance().font, "$state $unit", x + width / 2, y + 5, 0xFFFFEE)
    }

    override fun renderTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {}

    fun valueTooltip(): List<Component> = listOf(
        title.plainCopy().withStyle { it.withColor(HEADER_RGB.rgb) },
        Component.translatable("vs_clockwork.gui.creative_generator.range", limit, unit).withStyle(ChatFormatting.GRAY),
        scrollToModify.plainCopy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY),
        Component.translatable("vs_clockwork.gui.creative_generator.steps").withStyle(ChatFormatting.DARK_GRAY),
    )
}
