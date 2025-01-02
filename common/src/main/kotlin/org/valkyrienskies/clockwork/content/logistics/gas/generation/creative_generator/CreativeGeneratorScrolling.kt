package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.client.gui.Font
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.GuiUtil.withinRectangle
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame
import kotlin.math.roundToInt

class CreativeGeneratorScrolling(x: Int, y: Int) : ScrollingFrame(x, y, 159, 64) {
    override var padding = 4.0
    override var scrollSpeed: Double = 4.0


    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        for (element in scrollingElements) {
            val input = (element as CreativeGeneratorScrollingElement).input


            if (input.visible && withinRectangle(mouseX.roundToInt(),(mouseY-scroll).roundToInt(),input.x,input.y,input.width,input.height)) {
                return input.mouseScrolled(mouseX, mouseY, delta)
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta)
    }

    class CreativeGeneratorScrollingElement(val gasType: GasType, val font: Font, val input: ScrollInput) : ScrollingElement() {
        override val height = 18.0


        override fun renderElement(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float, visible: Boolean, scroll: Double) {

            val tab = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_ELEMENT

            input.x = x+79
            input.y = y+5
            input.visible = visible


            if (visible) {
                tab.render(ms, x, y)
                AbstractSimiScreen.drawString(ms, font, gasType.name, x+5, y+5,0xFFFFFF)
                AbstractSimiScreen.drawString(ms, font, input.state.toString()+" kg", x+79, y+5,0xFFFFFF)
                input.render(ms, mouseX, mouseY, partialTicks)
            }

        }
    }
}