package org.valkyrienskies.clockwork.util.gui

import com.simibubi.create.foundation.gui.AllGuiTextures
import net.createmod.catnip.gui.widget.AbstractSimiWidget
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

open class ScrollingFrame(x: Int, y: Int, w: Int, h: Int, private val scrollbarWidth: Int = 10) : AbstractSimiWidget(x, y, w, h) {
    val contentWidth get() = (width - scrollbarWidth).coerceAtLeast(0)
    protected open val contentHeight: Double
        get() = scrollingElements.sumOf { it.height } + padding * (scrollingElements.size - 1).coerceAtLeast(0)
    val minScroll get() = -(contentHeight - height).coerceAtLeast(0.0)
    val maxScroll get() = 0.0
    var scroll = 0.0
        set(value) { field = value.coerceIn(minScroll, maxScroll) }
    var currentScroll = 0.0
        private set
    protected val renderedScroll get() = currentScroll.roundToInt()
    protected val thumbHeight get() = (height.toDouble() * height / contentHeight.coerceAtLeast(1.0)).toInt().coerceIn(minOf(16, height), height)
    protected val thumbY get() = y + if (minScroll == 0.0) 0 else ((height - thumbHeight) * currentScroll / minScroll).roundToInt()
    protected var draggingScrollbar = false
        private set
    private var dragOffset = 0.0
    private var lastFrame = Util.getMillis()

    open var scrollSpeed = 1.0
    open var padding = 0.0
    var scrollingElements: MutableList<ScrollingElement> = mutableListOf()

    fun restoreScroll(value: Double) {
        scroll = value
        currentScroll = scroll
    }

    protected fun isMouseOverContent(mouseX: Double, mouseY: Double): Boolean {
        return isMouseOver(mouseX, mouseY) && mouseX < x + contentWidth
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (!isMouseOver(mouseX, mouseY)) return false
        scroll += scrollSpeed * delta
        return true
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button == 0 && active && isMouseOver(mouseX, mouseY) && mouseX >= x + contentWidth && scrollbarWidth > 0 && minScroll < 0.0) {
            dragOffset = if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) mouseY - thumbY else thumbHeight / 2.0
            draggingScrollbar = true
            dragTo(mouseY)
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun onDrag(mouseX: Double, mouseY: Double, dragX: Double, dragY: Double) {
        if (draggingScrollbar) dragTo(mouseY)
    }

    override fun onRelease(mouseX: Double, mouseY: Double) {
        draggingScrollbar = false
    }

    private fun dragTo(mouseY: Double) {
        scroll = (mouseY - y - dragOffset) / (height - thumbHeight).coerceAtLeast(1) * minScroll
        currentScroll = scroll
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (!isFocused) return false
        when (keyCode) {
            GLFW.GLFW_KEY_UP -> scroll += scrollSpeed
            GLFW.GLFW_KEY_DOWN -> scroll -= scrollSpeed
            GLFW.GLFW_KEY_PAGE_UP -> scroll += height
            GLFW.GLFW_KEY_PAGE_DOWN -> scroll -= height
            GLFW.GLFW_KEY_HOME -> scroll = maxScroll
            GLFW.GLFW_KEY_END -> scroll = minScroll
            else -> return super.keyPressed(keyCode, scanCode, modifiers)
        }
        return true
    }

    override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        scroll = scroll
        currentScroll = currentScroll.coerceIn(minScroll, maxScroll)
        val now = Util.getMillis()
        val elapsed = ((now - lastFrame) / 1000.0).coerceIn(0.0, 0.1)
        lastFrame = now
        currentScroll += (scroll - currentScroll) * (1.0 - exp(-18.0 * elapsed))
        if (abs(scroll - currentScroll) < 0.1) currentScroll = scroll
        graphics.enableScissor(x, y, x + contentWidth, y + height)
        renderContents(graphics, mouseX, mouseY, partialTicks)
        graphics.disableScissor()
        if (scrollbarWidth > 0 && minScroll < 0.0) renderScrollbar(graphics)
        else draggingScrollbar = false
    }

    protected open fun renderContents(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(0.0, renderedScroll.toDouble(), 0.0)
        var pastHeight = 0.0
        for (element in scrollingElements) {
            element.x = x
            element.y = (y + pastHeight).roundToInt()
            val elementY = element.y + renderedScroll
            val visible = elementY + element.height > y && elementY < y + height
            element.renderElement(graphics, mouseX, mouseY, partialTicks, visible, renderedScroll.toDouble())
            pastHeight += element.height + padding
        }
        pose.popPose()
    }

    protected open fun renderScrollbar(graphics: GuiGraphics) {
        val barX = x + width - 6
        val pad = AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_PAD
        graphics.blitRepeating(pad.location, barX, thumbY, pad.width, thumbHeight, pad.startX, pad.startY, pad.width, pad.height)
        AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_TOP.render(graphics, barX, thumbY)
        if (thumbHeight > 16) AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_MID.render(graphics, barX, thumbY + thumbHeight / 2 - 4)
        AllGuiTextures.STOCK_KEEPER_REQUEST_SCROLL_BOT.render(graphics, barX, thumbY + thumbHeight - 5)
    }

    abstract class ScrollingElement {
        open val height: Double = 0.0
        var x: Int = 0
        var y: Int = 0

        abstract fun renderElement(ms: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, visible: Boolean, scroll: Double)
    }
}
