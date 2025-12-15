package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket

class GrabssembleTool : GravitronToolBase() {

    override fun handleRightClick(isRegular: Boolean): Boolean {
        updateTargetPos()
        if (clickedLocation != null && clickedPos != null) {
            sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GRABSSEMBLE))
        }
        return true
    }

    override fun handleLeftClick(
        regular: Boolean
    ): Boolean {
        return false
    }
}