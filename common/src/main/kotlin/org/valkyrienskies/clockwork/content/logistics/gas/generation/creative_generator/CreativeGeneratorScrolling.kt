package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.kelvin.api.GasType

class CreativeGeneratorScrolling(
    x: Int, y: Int, width: Int, height: Int,
    private val font: Font,
    values: MutableMap<GasType, Int>,
) : ScrollingFrame(x, y, width, height) {
    private val rows = values.keys.sortedWith(compareBy<GasType> { it.name }.thenBy { it.resourceLocation.toString() }).map { gas ->
        Row(gas, CreativeGeneratorValueInput(0, 0, CreativeGeneratorScreen.MAX_GAS, "g").apply {
            titled(Component.literal(gas.name))
            state = values[gas] ?: 0
            calling { values[gas] = it }
        })
    }
    override val contentHeight get() = (rows.size * ROW_HEIGHT - 4).coerceAtLeast(0).toDouble()
    override var scrollSpeed = ROW_HEIGHT.toDouble()

    override fun tick() {
        rows.forEach { it.input.tick() }
    }

    fun reset() {
        rows.forEach { it.input.state = 0; it.input.onChanged() }
    }

    private fun layout() {
        rows.forEachIndexed { index, row ->
            row.input.x = x + contentWidth - row.input.width
            row.input.y = y + index * ROW_HEIGHT + renderedScroll
        }
    }

    private fun rowAt(mouseX: Double, mouseY: Double): Row? {
        if (!isMouseOverContent(mouseX, mouseY)) return null
        val localY = mouseY - y - renderedScroll
        val index = (localY / ROW_HEIGHT).toInt()
        if (localY < 0 || localY % ROW_HEIGHT >= ROW_HEIGHT - 4) return null
        return rows.getOrNull(index)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        layout()
        val input = rowAt(mouseX, mouseY)?.input
        if (input != null && input.isMouseOver(mouseX, mouseY)) {
            input.mouseScrolled(mouseX, mouseY, delta)
        } else {
            super.mouseScrolled(mouseX, mouseY, delta)
        }
        return true
    }

    override fun renderContents(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        layout()
        val hovered = rowAt(mouseX.toDouble(), mouseY.toDouble())
        for (row in rows) {
            val rowY = row.input.y
            if (rowY + ROW_HEIGHT <= y || rowY >= y + height) continue
            val tab = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_ELEMENT.location
            graphics.blitNineSliced(tab, x, rowY, contentWidth - row.input.width, 18, 4, 76, 18, 0, 0)
            graphics.blit(tab, row.input.x, rowY, 76f, 0f, 52, 18, 256, 256)
            graphics.blit(row.gas.iconLocation, x + 5, rowY + 1, 0f, 0f, 16, 16, 16, 16)
            val nameWidth = row.input.x - x - 29
            val name = if (font.width(row.gas.name) <= nameWidth) row.gas.name else font.plainSubstrByWidth(row.gas.name, nameWidth - font.width("…")) + "…"
            graphics.drawString(font, name, x + 25, rowY + 5, 0xFFFFEE)
            row.input.render(graphics, if (row == hovered) mouseX else -1, if (row == hovered) mouseY else -1, partialTicks)
        }
    }

    fun renderHoverTooltip(graphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (draggingScrollbar) return
        val row = rowAt(mouseX.toDouble(), mouseY.toDouble()) ?: return
        if (row.input.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
            graphics.renderComponentTooltip(font, row.input.valueTooltip(), mouseX, mouseY)
        } else {
            graphics.renderTooltip(font, Component.literal(row.gas.name), mouseX, mouseY)
        }
    }

    private data class Row(val gas: GasType, val input: CreativeGeneratorValueInput)

    companion object {
        private const val ROW_HEIGHT = 22
    }
}
