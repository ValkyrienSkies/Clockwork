package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

class VdexCoordinateBox(font: Font, x: Int, y: Int, width: Int, height: Int, label: Component) : EditBox(font, x, y, width, height, label) {
    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val unshadowed = object : GuiGraphics(Minecraft.getInstance(), graphics.bufferSource()) {
            override fun drawString(font: Font, text: String?, x: Int, y: Int, color: Int) = graphics.drawString(font, text, x, y, color, false)
            override fun drawString(font: Font, text: Component, x: Int, y: Int, color: Int) = graphics.drawString(font, text, x, y, color, false)
            override fun drawString(font: Font, text: FormattedCharSequence, x: Int, y: Int, color: Int) = graphics.drawString(font, text, x, y, color, false)
        }
        unshadowed.pose().last().pose().set(graphics.pose().last().pose())
        unshadowed.pose().last().normal().set(graphics.pose().last().normal())
        super.renderWidget(unshadowed, mouseX, mouseY, partialTick)
    }
}
