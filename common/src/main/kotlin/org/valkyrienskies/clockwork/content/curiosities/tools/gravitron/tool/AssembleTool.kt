package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronGrabPacket

class AssembleTool : GravitronToolBase() {

    override fun handleRightClick(isRegular: Boolean): Boolean {
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, ASSEMBLE))
        }
        return true
    }

    override fun handleLeftClick(
        regular: Boolean
    ): Boolean {
        return false
    }
}