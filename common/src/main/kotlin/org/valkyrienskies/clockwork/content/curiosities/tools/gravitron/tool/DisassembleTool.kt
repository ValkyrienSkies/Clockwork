package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.tool

import org.valkyrienskies.clockwork.ClockworkPackets.Companion.sendToServer
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronDisassemblyPacket

class DisassembleTool : GravitronToolBase() {

    override fun handleRightClick(isRegular: Boolean): Boolean {
        updateTargetPos()
        if (clickedPos != null) {
            sendToServer(GravitronDisassemblyPacket(clickedPos!!))
        }
        return true
    }

    override fun handleLeftClick(
        regular: Boolean
    ): Boolean {
        return false
    }

}