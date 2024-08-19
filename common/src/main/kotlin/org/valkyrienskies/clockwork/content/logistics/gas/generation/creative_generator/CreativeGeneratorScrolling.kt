package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame

class CreativeGeneratorScrolling(x: Int, y: Int) : ScrollingFrame(x, y, 159, 64) {
    override var padding = 4.0
    override var scrollSpeed: Double = 2.0



    class CreativeGeneratorScrollingElement(val input: ScrollInput) : ScrollingElement {
        override val height = 18.0


        override fun renderElement(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float, x: Int, y: Int, visible: Boolean) {

            val tab = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_TAB

            input.x = x
            input.y = y

            if (visible) {
                tab.render(ms, x, y)
                input.render(ms, mouseX, mouseY, partialTicks)
            }
        }
    }
}