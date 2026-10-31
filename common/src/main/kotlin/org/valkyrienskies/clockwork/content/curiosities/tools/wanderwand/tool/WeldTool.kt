package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket
import org.valkyrienskies.mod.common.VSClientGameUtils

class WeldTool(): WanderwandToolBase() {
    override fun handleRightClick(crouching: Boolean): Boolean {
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            //ClockworkPackets.sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GravitronToolBase.GRAB))
            if (Minecraft.getInstance().player != null) {
                val cLevel = Minecraft.getInstance().level
                val ship = VSClientGameUtils.getClientShip(clickedPos!!.x.toDouble(), clickedPos!!.y.toDouble(), clickedPos!!.z.toDouble())
                if (ship == null) {
                    val message = Component.literal("Can't weld a non-ship!")

                    message.setStyle(Style.EMPTY.withColor(ChatFormatting.RED).withItalic(true))

                    Minecraft.getInstance().player!!.displayClientMessage(message, true)
                    clickedPos = lastClickedPos
                    clickedLocation = lastClickedLocation
                    lastClickedLocation = null
                    lastClickedPos = null
                    return false
                }
                if (lastClickedPos == null) {
                    val message = Component.literal("Selected target at: $clickedPos")

                    message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(true))

                    Minecraft.getInstance().player!!.displayClientMessage(message, true)
                    ClockworkPackets.sendToServer(WandSelectionPacket(clickedPos!!, null, ToolType.WELD, true, clickedDirection!!.ordinal))
                    return true
                }
                val message = Component.literal("Welding! ($lastClickedPos -> $clickedPos)")

                message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true))

                Minecraft.getInstance().player!!.displayClientMessage(message, true)

                ClockworkPackets.sendToServer(WandSelectionPacket(lastClickedPos!!, clickedPos!!, ToolType.WELD, false, clickedDirection!!.ordinal))
                lastClickedPos = null
                lastClickedLocation = null
                clickedPos = null
                clickedLocation = null
                lastClickedDirection = null
                clickedDirection = null
                return true
            }
        }
        return false
    }
}
