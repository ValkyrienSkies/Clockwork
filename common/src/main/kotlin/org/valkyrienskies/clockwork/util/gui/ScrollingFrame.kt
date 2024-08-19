package org.valkyrienskies.clockwork.util.gui

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.util.gui.GuiUtil.withinRectangle
import kotlin.math.roundToInt

open class ScrollingFrame(x: Int, y: Int, w: Int, h: Int): AbstractSimiWidget(x,y,w,h) {

    var minScroll = 0.0
    var maxScroll = 0.0

    var scroll = 0.0

    open var scrollSpeed = 1.0
    open var padding = 0.0

    var scrollingElements: MutableList<ScrollingElement> = mutableListOf()

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {

        var newScroll = Mth.clamp(scroll+(scrollSpeed*delta),minScroll,maxScroll)
        if (newScroll==scroll) return false
        scroll = newScroll
        return true


    }

    override fun renderButton(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {


        var pastHeight = 0.0
        val frameY = y+scroll
        for (element in scrollingElements) {

            val elementY = frameY+pastHeight
            val visible = elementY+element.height>=y && elementY<=y+height


            element.renderElement(ms, mouseX, mouseY, partialTicks, x, elementY.roundToInt(), visible)

            pastHeight+=element.height+padding
        }


    }


    interface ScrollingElement {
        val height: Double


        abstract fun renderElement(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float, x: Int, y: Int, visible: Boolean)
    }

}