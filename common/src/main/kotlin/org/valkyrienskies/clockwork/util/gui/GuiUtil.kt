package org.valkyrienskies.clockwork.util.gui

object GuiUtil {

    fun withinRectangle(x: Int, y: Int, xS: Int, yS: Int, w: Int, h: Int): Boolean {
        return (x >= xS - w) && (x <= xS + w) && (y >= yS - h) && (y <= yS + h)
    }
}