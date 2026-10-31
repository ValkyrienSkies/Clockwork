package org.valkyrienskies.clockwork.content.logistics.gas.filter

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.GuiUtil.withinRectangle
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame
import kotlin.math.roundToInt

class FilterScrolling(x: Int, y: Int) : ScrollingFrame(x, y, 206, 47) {
    override var padding = 4.0
    override var scrollSpeed: Double = 4.0



    override fun onClick(mouseX: Double, mouseY: Double) {

        for (element in scrollingElements) {
            val button = (element as FilterScrollingElement).button


            if (button.visible && withinRectangle(mouseX.roundToInt(),(mouseY-scroll).roundToInt(),button.x,button.y,button.width,button.height)) {
                return button.onClick(mouseX, mouseY)
            }
        }

        super.onClick(mouseX, mouseY)
    }

    class FilterScrollingElement(val gasType: GasType, val font: Font, val button: IconButton) : ScrollingElement() {
        override val height = button.height.toDouble()


        override fun renderElement(ms: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, visible: Boolean, scroll: Double) {

            val tab = ClockworkGuiTextures.GAS_FILTER_ELEMENT

            button.x = x+79
            button.y = y-1
            button.visible = visible

            if (visible) {
                tab.render(ms, x, y)
                ms.drawString(font, gasType.name, x+5, y+5,0xFFFFFF)
                button.render(ms, mouseX, (mouseY-scroll).roundToInt(), partialTicks)
            }

        }
    }
}
