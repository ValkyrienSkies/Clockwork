package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.HitResult
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

    override fun updateTargetPos() {
        lastClickedPos = clickedPos
        lastClickedLocation = clickedLocation
        lastClickedDirection = clickedDirection

        val player = Minecraft.getInstance().player

        val trace = RaycastHelper.rayTraceRange(
            player!!.level(), player, 15.0
        ) ?: return


        clickedPos = if (trace.type == HitResult.Type.BLOCK) trace.blockPos else BlockPos.containing(RaycastHelper.getTraceTarget(player, 4.0, player.eyePosition))
        clickedLocation = if (trace.type == HitResult.Type.BLOCK) trace.location else RaycastHelper.getTraceTarget(player, 4.0, player.eyePosition)

    }
}