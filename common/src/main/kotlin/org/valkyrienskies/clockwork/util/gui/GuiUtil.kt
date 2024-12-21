package org.valkyrienskies.clockwork.util.gui

object GuiUtil {

    //This checks for a rectangle, whose position is in the center
    fun withinCenteredRectangle(x: Int, y: Int, xR: Int, yR: Int, w: Int, h: Int): Boolean {
        return (x >= xR - w/2) && (x <= xR + w/2) && (y >= yR - h/2) && (y <= yR + h/2)
    }

    //This checks for a rectangle, whose position is at its top left
    fun withinRectangle(x: Int, y: Int, xR: Int, yR: Int, w: Int, h: Int): Boolean {
        return (x >= xR) && (x <= xR + w) && (y >= yR) && (y <= yR + h)
    }
}