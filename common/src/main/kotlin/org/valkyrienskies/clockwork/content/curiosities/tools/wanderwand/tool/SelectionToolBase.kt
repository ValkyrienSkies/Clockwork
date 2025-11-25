package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket

open class SelectionToolBase: WanderwandToolBase() {

    override fun handleLeftClick(): Boolean {
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            ClockworkPackets.sendToServer(WandSelectionPacket(clickedPos!!, null, ToolType.SELECT, true))
            lastClickedPos = null
            return true
        }
        lastClickedPos = null
        return false
    }
}