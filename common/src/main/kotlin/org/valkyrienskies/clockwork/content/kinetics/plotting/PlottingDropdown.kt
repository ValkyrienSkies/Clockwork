package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.max

internal data class PlottingChoice(val name: Component, val id: String)

internal class PlottingDropdown(
    private val font: Font,
    private val x: Int,
    private val y: Int,
    private val width: Int,
    private val rows: Int,
    private val choices: List<PlottingChoice>,
    selected: Int,
    private val choose: (Int) -> Unit,
    private val close: () -> Unit,
) {
    private var results = choices.indices.toList()
    private var highlighted = selected.coerceIn(0, max(0, choices.lastIndex))
    private var offset = (highlighted - rows + 1).coerceAtLeast(0)
    private val height = 27 + rows * 20
    private val search = EditBox(font, x + 5, y + 5, width - 10, 18, tr("search")).apply {
        setMaxLength(128)
        setHint(tr("search"))
        setFocused(true)
        setResponder { query ->
            results = choices.indices.filter { choices[it].name.string.contains(query, true) || choices[it].id.contains(query, true) }
            this@PlottingDropdown.highlighted = 0
            this@PlottingDropdown.offset = 0
        }
    }

    fun tick() = search.tick()

    fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        g.pose().pushPose()
        g.pose().translate(0.0, 0.0, 500.0)
        g.fill(x, y, x + width, y + height, 0xff202020.toInt())
        g.renderOutline(x, y, width, height, 0xffa0a0a0.toInt())
        search.render(g, mouseX, mouseY, partialTicks)
        if (results.isEmpty()) g.drawString(font, tr("no_matches"), x + 6, y + 31, 0xffa0a0a0.toInt(), false)
        for (row in 0 until rows) {
            val index = offset + row
            val choice = results.getOrNull(index)?.let(choices::get) ?: break
            val rowY = y + 25 + row * 20
            val hovered = mouseX in x + 2 until x + width - 5 && mouseY in rowY until rowY + 20
            if (hovered || index == highlighted) g.fill(x + 2, rowY, x + width - 5, rowY + 20, 0xff4a4a4a.toInt())
            g.drawString(font, font.plainSubstrByWidth(choice.name.string, width - 15), x + 6, rowY + 6, 0xffeeeeee.toInt(), false)
        }
        if (results.size > rows) {
            val track = rows * 20
            val thumb = max(8, track * rows / results.size)
            val thumbY = y + 25 + (track - thumb) * offset / (results.size - rows)
            g.fill(x + width - 4, thumbY, x + width - 2, thumbY + thumb, 0xffa0a0a0.toInt())
        }
        g.pose().popPose()
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) close()
        else if (button == 0 && mouseY >= y + 25) {
            val index = offset + ((mouseY - y - 25) / 20).toInt()
            results.getOrNull(index)?.let { choose(it); close() }
        } else search.mouseClicked(mouseX, mouseY, button)
        return true
    }

    fun scroll(delta: Double): Boolean {
        offset = (offset - delta.toInt()).coerceIn(0, max(0, results.size - rows))
        return true
    }

    fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        when (key) {
            GLFW.GLFW_KEY_ESCAPE -> close()
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> results.getOrNull(highlighted)?.let { choose(it); close() }
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                highlighted = (highlighted + if (key == GLFW.GLFW_KEY_DOWN) 1 else -1).coerceIn(0, max(0, results.lastIndex))
                if (highlighted < offset) offset = highlighted
                if (highlighted >= offset + rows) offset = highlighted - rows + 1
            }
            else -> search.keyPressed(key, scanCode, modifiers)
        }
        return true
    }

    fun charTyped(character: Char, modifiers: Int): Boolean { search.charTyped(character, modifiers); return true }

    private fun tr(key: String) = Component.translatable("gui.vs_clockwork.plotting.$key")
}
