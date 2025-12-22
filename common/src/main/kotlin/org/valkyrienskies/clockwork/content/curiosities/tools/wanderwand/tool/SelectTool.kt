package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.utility.RaycastHelper
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionPacket
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.isChunkInShipyard

class SelectTool(): SelectionToolBase() {

    override fun handleRightClick(crouching: Boolean): Boolean {
        if (crouching) return false
        val player = Minecraft.getInstance().player

        val trace = RaycastHelper.rayTraceRange(player!!.level(), player, 15.0)
        println("$trace ${trace.type} ${trace.blockPos}")
        if (trace != null && trace.type == HitResult.Type.BLOCK) {
            //if phys infuser, don't let select
            if (player.level().getBlockEntity(trace.blockPos) is PhysicsInfuserBlockEntity) return false
            //if on ship, don't let select
            if (player.level().isBlockInShipyard(trace.blockPos)) return false

        }
        updateTargetPos()
        if (clickedPos != null && clickedLocation != null) {
            //ClockworkPackets.sendToServer(GravitronGrabPacket(clickedPos!!, clickedLocation!!, GravitronToolBase.GRAB))
            if (Minecraft.getInstance().player != null) {

                if (lastClickedPos == null) {
                    val message = Component.literal("Selected Corner One: $clickedPos")

                    message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withItalic(true))

                    Minecraft.getInstance().player!!.displayClientMessage(message, true)

                    ClockworkPackets.sendToServer(WandSelectionPacket(clickedPos!!, null, ToolType.SELECT, false))
                    return true
                }
                val message = Component.literal("Selection expanded! ($lastClickedPos -> $clickedPos)")

                message.setStyle(Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true))

                Minecraft.getInstance().player!!.displayClientMessage(message, true)

                ClockworkPackets.sendToServer(WandSelectionPacket(lastClickedPos!!, clickedPos!!, ToolType.SELECT, false))
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
