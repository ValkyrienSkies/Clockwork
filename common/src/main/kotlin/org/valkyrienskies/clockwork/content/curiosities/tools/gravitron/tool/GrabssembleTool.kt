package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket

class GrabssembleTool : GravitronToolBase() {

    override fun handleRightClick(): Boolean {
        updateTargetPos()
        if (clickedLocation != null && clickedPos != null) {
            sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GRABSSEMBLE))
        }
        return true
    }
}