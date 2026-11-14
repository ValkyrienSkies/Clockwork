package org.valkyrienskies.clockwork.content.kinetics.plotting

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import kotlin.math.max

internal data class PlottingMenuAction(val name: Component, val enabled: Boolean = true,
    val destructive: Boolean = false, val action: () -> Unit)

internal class PlottingContextMenu(mouseX: Int, mouseY: Int, requestedWidth: Int, screenWidth: Int, screenHeight: Int,
    private val actions: List<PlottingMenuAction>, private val close: () -> Unit) {
    val width = requestedWidth.coerceIn(1, max(1, screenWidth - 8))
    private val rows = minOf(actions.size, max(1, (screenHeight - 16) / 18))
    val height = rows * 18 + 8
    val x = mouseX.coerceIn(4, max(4, screenWidth - width - 4))
    val y = mouseY.coerceIn(4, max(4, screenHeight - height - 4))
    private var offset = 0
    private var highlighted = -1
    private var lastMouse: Pair<Int, Int>? = null

    private fun contains(mouseX: Double, mouseY: Double) = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    private fun rowAt(mouseX: Double, mouseY: Double): Int =
        if (mouseX >= x + 4 && mouseX < x + width - 4 && mouseY >= y + 4 && mouseY < y + height - 4)
            offset + ((mouseY - y - 4) / 18).toInt() else -1

    fun render(g: GuiGraphics, font: Font, mouseX: Int, mouseY: Int) {
        if (lastMouse != (mouseX to mouseY)) {
            highlighted = rowAt(mouseX.toDouble(), mouseY.toDouble())
            lastMouse = mouseX to mouseY
        }
        g.pose().pushPose()
        g.pose().translate(0.0, 0.0, 600.0)
        g.fill(x, y, x + width, y + height, 0xf0100010.toInt())
        g.fillGradient(x, y + 1, x + 1, y + height - 1, 0x505000ff, 0x5028007f)
        g.fillGradient(x + width - 1, y + 1, x + width, y + height - 1, 0x505000ff, 0x5028007f)
        g.fill(x + 1, y, x + width - 1, y + 1, 0x505000ff)
        g.fill(x + 1, y + height - 1, x + width - 1, y + height, 0x5028007f)
        for (row in 0 until rows) {
            val index = offset + row
            val entry = actions[index]
            val rowY = y + 4 + row * 18
            if (index == highlighted && entry.enabled) g.fill(x + 3, rowY, x + width - 3, rowY + 18, 0x604c426b)
            val color = when { !entry.enabled -> 0xff666666.toInt(); entry.destructive -> 0xffff8888.toInt()
                index == highlighted -> 0xffffffaa.toInt(); else -> 0xffeeeeee.toInt() }
            g.drawString(font, font.plainSubstrByWidth(entry.name.string, max(0, width - 16)), x + 8, rowY + 5, color, true)
        }
        if (actions.size > rows) {
            val track = rows * 18
            val thumb = max(4, track * rows / actions.size)
            val thumbY = y + 4 + (track - thumb) * offset / (actions.size - rows)
            g.fill(x + width - 3, thumbY, x + width - 2, thumbY + thumb, 0xffa0a0a0.toInt())
        }
        g.pose().popPose()
    }

    fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!contains(mouseX, mouseY) || button == 1) close()
        else if (button == 0) choose(rowAt(mouseX, mouseY))
        return true
    }

    private fun choose(index: Int) {
        val entry = actions.getOrNull(index)?.takeIf { it.enabled } ?: return
        close()
        entry.action()
    }

    fun scroll(delta: Double): Boolean {
        offset = (offset - delta.toInt()).coerceIn(0, max(0, actions.size - rows))
        highlighted = -1
        lastMouse = null
        return true
    }

    fun keyPressed(key: Int): Boolean {
        when (key) {
            GLFW.GLFW_KEY_ESCAPE -> close()
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> choose(highlighted)
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                val direction = if (key == GLFW.GLFW_KEY_DOWN) 1 else -1
                var index = if (highlighted < 0 && direction < 0) 0 else highlighted
                repeat(actions.size) {
                    index = (index + direction).mod(actions.size)
                    if (actions[index].enabled) {
                        highlighted = index
                        if (index < offset) offset = index
                        if (index >= offset + rows) offset = index - rows + 1
                        return true
                    }
                }
            }
        }
        return true
    }
}
