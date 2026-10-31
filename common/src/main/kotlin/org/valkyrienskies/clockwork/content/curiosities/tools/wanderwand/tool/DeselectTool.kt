package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket

class DeselectTool(): SelectionToolBase() {

    override fun handleRightClick(crouching: Boolean): Boolean {
        if (crouching) return false
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            //ClockworkPackets.sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GravitronToolBase.GRAB))
            if (Minecraft.getInstance().player != null) {

                if (lastClickedPos == null) {
                    val message = Component.literal("Selected Corner One: $clickedPos")

                    message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(true))

                    Minecraft.getInstance().player!!.displayClientMessage(message, true)
                    ClockworkPackets.sendToServer(WandSelectionPacket(clickedPos!!, null, ToolType.DESELECT, false))
                    return true
                }
                val message = Component.literal("Selection subtracted! ($lastClickedPos -> $clickedPos)")

                message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true))

                Minecraft.getInstance().player!!.displayClientMessage(message, true)

                ClockworkPackets.sendToServer(WandSelectionPacket(lastClickedPos!!, clickedPos!!, ToolType.DESELECT, false))
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
