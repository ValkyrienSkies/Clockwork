package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDestroyPacket

class DisassembleTool : GravitronToolBase() {
    override fun handleRightClick(): Boolean {
        updateTargetPos()

        sendToServer(GravitronDestroyPacket(clickedPos))

        return true
    }
}