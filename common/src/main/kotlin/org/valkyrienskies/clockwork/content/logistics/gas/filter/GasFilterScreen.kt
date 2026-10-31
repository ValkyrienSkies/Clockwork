package org.valkyrienskies.clockwork.content.logistics.gas.filter

import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.createmod.catnip.gui.AbstractSimiScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

abstract class GasFilterScreen(
    title: Component,
    protected val filter: MutableSet<GasType>,
    protected var blacklist: Boolean,
    private val allowListHint: Component = Component.translatable("vs_clockwork.gui.gas_filter.allow_list_hint"),
    private val denyListHint: Component = Component.translatable("vs_clockwork.gui.gas_filter.deny_list_hint"),
) : AbstractSimiScreen(title) {
    private lateinit var grid: GasGrid
    private lateinit var allowButton: IconButton
    private lateinit var denyButton: IconButton

    override fun init() {
        setWindowSize(214, 104)
        super.init()
        grid = GasGrid(guiLeft + 6, guiTop + 21)
        addRenderableWidget(grid)

        denyButton = IconButton(guiLeft + 12, guiTop + 80, AllIcons.I_BLACKLIST)
        denyButton.setToolTip(denyListHint)
        denyButton.withCallback<IconButton> {
            blacklist = true
            updateButtons()
        }
        allowButton = IconButton(guiLeft + 32, guiTop + 80, AllIcons.I_WHITELIST)
        allowButton.setToolTip(allowListHint)
        allowButton.withCallback<IconButton> {
            blacklist = false
            updateButtons()
        }
        addRenderableWidget(denyButton)
        addRenderableWidget(allowButton)
        updateButtons()

        val confirm = IconButton(guiLeft + 181, guiTop + 80, AllIcons.I_CONFIRM)
        confirm.setToolTip(Component.translatable("gui.done"))
        confirm.withCallback<IconButton> { onClose() }
        addRenderableWidget(confirm)
    }

    private fun updateButtons() {
        denyButton.active = !blacklist
        allowButton.active = blacklist
    }

    override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        ClockworkGuiTextures.GAS_FILTER_FRAME.render(graphics, guiLeft, guiTop + 20)
        ClockworkGuiTextures.GAS_FILTER_TAB.render(graphics, guiLeft, guiTop)
        graphics.drawCenteredString(font, title, guiLeft + 103, guiTop + 6, 0xFFFFFF)
        val key = if (blacklist) "create.gui.filter.deny_list" else "create.gui.filter.allow_list"
        graphics.drawString(font, Component.translatable(key), guiLeft + 57, guiTop + 85, 0x404040, false)
    }

    override fun renderWindowForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        grid.gasAt(mouseX.toDouble(), mouseY.toDouble())?.let { gas ->
            graphics.renderTooltip(font, Component.literal(gas.name), mouseX, mouseY)
        }
    }

    private inner class GasGrid(x: Int, y: Int) : ScrollingFrame(x, y, 200, 47) {
        private val gases = GasTypeRegistry.GAS_TYPES.values.sortedBy { it.resourceLocation.toString() }
        override var scrollSpeed = 20.0

        init {
            scrollingElements = gases.chunked(COLUMNS).map { row -> GasRow(row) }.toMutableList()
            minScroll = -max(scrollingElements.size * CELL_SIZE - height, 0).toDouble()
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
            if (!isMouseOver(mouseX, mouseY)) return false
            super.mouseScrolled(mouseX, mouseY, delta)
            return true
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            val gas = gasAt(mouseX, mouseY) ?: return
            if (!filter.remove(gas)) filter.add(gas)
        }

        fun gasAt(mouseX: Double, mouseY: Double): GasType? {
            if (!isMouseOver(mouseX, mouseY)) return null
            val localX = mouseX - x
            val localY = mouseY - y - currentScroll
            if (localY < 0.0) return null
            val column = floor(localX / CELL_SIZE).toInt()
            val row = floor(localY / CELL_SIZE).toInt()
            return gases.getOrNull(row * COLUMNS + column)
        }

        override fun renderWidget(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.renderWidget(graphics, mouseX, mouseY, partialTicks)
            if (minScroll < 0.0) {
                val contentHeight = scrollingElements.size * CELL_SIZE
                val thumbHeight = max(6, height * height / contentHeight)
                val thumbY = y + ((height - thumbHeight) * currentScroll / minScroll).roundToInt()
                graphics.fill(x + width + 1, y, x + width + 3, y + height, 0xFF49443C.toInt())
                graphics.fill(x + width + 1, thumbY, x + width + 3, thumbY + thumbHeight, 0xFFDDD5C3.toInt())
            }
        }

        private inner class GasRow(private val gases: List<GasType>) : ScrollingElement() {
            override val height = CELL_SIZE.toDouble()

            override fun renderElement(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, visible: Boolean, scroll: Double) {
                if (!visible) return
                val hovered = gasAt(mouseX.toDouble(), mouseY.toDouble())
                for ((column, gas) in gases.withIndex()) {
                    val px = x + column * CELL_SIZE
                    val selected = gas in filter
                    val color = if (selected) 0xFFD8B766.toInt() else 0xFF49443C.toInt()
                    graphics.fill(px, y, px + 19, y + 19, color)
                    graphics.fill(px + 1, y + 1, px + 18, y + 18, 0xFF746B59.toInt())
                    graphics.blit(gas.iconLocation, px + 2, y + 2, 0f, 0f, 16, 16, 16, 16)
                    if (gas == hovered) graphics.fill(px + 1, y + 1, px + 18, y + 18, 0x40FFFFFF)
                    if (selected) graphics.fill(px + 14, y + 14, px + 18, y + 18, 0xFFD8B766.toInt())
                }
            }
        }
    }

    companion object {
        private const val COLUMNS = 10
        private const val CELL_SIZE = 20
    }
}
