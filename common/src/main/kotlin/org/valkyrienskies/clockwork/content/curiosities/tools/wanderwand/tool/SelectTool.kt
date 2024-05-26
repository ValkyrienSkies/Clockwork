package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.render.SuperRenderTypeBuffer
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket

class SelectTool(): WanderwandToolBase() {

    override fun handleRightClick(crouching: Boolean): Boolean {
        if (crouching) {
            return false
        }
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            //ClockworkPackets.sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GravitronToolBase.GRAB))
            if (Minecraft.getInstance().player != null) {

                if (lastClickedPos == null) {
                    val message = TextComponent("Selected Corner One: $clickedPos")

                    message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(true))

                    Minecraft.getInstance().player!!.displayClientMessage(message, true)
                    ClockworkPackets.sendToServer(WandSelectionPacket(clickedPos!!, null, ToolType.SELECT, false))
                    return true
                }
                val message = TextComponent("Selection expanded! ($lastClickedPos -> $clickedPos)")

                message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true))

                Minecraft.getInstance().player!!.displayClientMessage(message, true)

                ClockworkPackets.sendToServer(WandSelectionPacket(lastClickedPos!!, clickedPos!!, ToolType.SELECT, false))
                return true
            }
        }
        return false
    }

}