package org.valkyrienskies.clockwork.content.events

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.GravitronInputPacket

object ClockworkInputEvents {
    fun onClickInputCW(isUse: Boolean, isAttack: Boolean): InteractionResult {
        if (isAttack) {
            val player = Minecraft.getInstance().player
            if (player != null) {
                /*
                if (player.getItemInHand(InteractionHand.MAIN_HAND).item is AreaDesignatorItem) {
                    val item = player.getItemInHand(InteractionHand.MAIN_HAND).item as AreaDesignatorItem
                    // item.onAttack(player)
                    return InteractionResult.SUCCESS
                }
                 */
                // Handle Gravitron
                if (player.getItemInHand(InteractionHand.MAIN_HAND).item == ClockworkItems.GRAVITRON.get()) {
                    ClockworkPackets.sendToServer(GravitronInputPacket(true))
                    return InteractionResult.SUCCESS
                }
            }
        }
        return InteractionResult.PASS
    }

    fun onMouseScrolled(delta: Double): Boolean {
        if (Minecraft.getInstance().screen != null) return false

        val player = Minecraft.getInstance().player
        if (player != null) {
            // TODO: Only cancel if player is grabbing a ship
            if (player.getItemInHand(InteractionHand.MAIN_HAND).item == ClockworkItems.GRAVITRON.get()) {
                ClockworkPackets.sendToServer(GravitronInputPacket(delta))
                return true
            }
        }

        return false
    }
}
