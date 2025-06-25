package org.valkyrienskies.clockwork.util.gui

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.util.gui.GuiUtil.withinRectangle
import kotlin.math.max
import kotlin.math.roundToInt

open class ScrollingFrame(x: Int, y: Int, w: Int, h: Int): AbstractSimiWidget(x,y,w,h) {

    var minScroll = 0.0
    var maxScroll = 0.0

    var scroll = 0.0
    var currentScroll = 0.0

    open var scrollSpeed = 1.0
    open var padding = 0.0

    val scrollLerpSpeed = 2.0


    var scrollingElements: MutableList<ScrollingElement> = mutableListOf()


    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {

        var newScroll = Mth.clamp(scroll+(scrollSpeed*delta),minScroll,maxScroll)
        if (newScroll==scroll) return false
        scroll = newScroll
        return true


    }

    override fun renderWidget(ms: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        currentScroll += Mth.clamp(scroll-currentScroll,-partialTicks*scrollLerpSpeed, partialTicks*scrollLerpSpeed)

        ms.pose().translate(0.0,currentScroll,0.0)

        var pastHeight = 0.0
        val frameY = y+currentScroll
        for (element in scrollingElements) {


            val elementY = frameY+pastHeight
            val visible = elementY+element.height>=y && elementY<=y+height

            element.x = x
            element.y = (y+pastHeight).roundToInt()
            element.renderElement(ms, mouseX, mouseY, partialTicks, visible, scroll)

            pastHeight+=element.height+padding
        }
        minScroll = -max(pastHeight-height,0.0)

    }


    abstract class ScrollingElement() {
        open val height: Double = 0.0
        var x: Int = 0
        var y: Int = 0

        abstract fun renderElement(ms: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, visible: Boolean, scroll: Double)
    }

}