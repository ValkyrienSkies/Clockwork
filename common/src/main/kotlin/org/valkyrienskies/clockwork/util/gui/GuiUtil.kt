package org.valkyrienskies.clockwork.util.gui

object GuiUtil {

    fun withinCenteredRectangle(x: Int, y: Int, xS: Int, yS: Int, w: Int, h: Int): Boolean {
        return (x >= xS - w/2) && (x <= xS + w/2) && (y >= yS - h/2) && (y <= yS + h/2)
    }

    fun withinRectangle(x: Int, y: Int, xS: Int, yS: Int, w: Int, h: Int): Boolean {
        return (x >= xS) && (x <= xS + w) && (y >= yS) && (y <= yS + h)
    }
}