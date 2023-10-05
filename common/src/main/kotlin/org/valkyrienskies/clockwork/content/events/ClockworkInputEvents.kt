package org.valkyrienskies.clockwork.content.events

import io.github.fabricators_of_create.porting_lib.util.KeyBindingHelper
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorItem

object ClockworkInputEvents {
    fun onClickInputCW(button: Int, action: Int, mods: Int): InteractionResult {
        val mc = Minecraft.getInstance()
        if (mc.screen != null) return InteractionResult.PASS
        val use = KeyBindingHelper.getKeyCode(mc.options.keyUse).value
        val attack = KeyBindingHelper.getKeyCode(mc.options.keyAttack).value
        val isUse = button == use
        val isAttack = button == attack
        if (isAttack) {
            if (Minecraft.getInstance().player != null) {
                val player: Player? = Minecraft.getInstance().player
                if (player!!.getItemInHand(InteractionHand.MAIN_HAND).item is AreaDesignatorItem) {
                    val item = player.getItemInHand(InteractionHand.MAIN_HAND).item as AreaDesignatorItem
                    // item.onAttack(player)
                    return InteractionResult.SUCCESS
                }
            }
        }
        return InteractionResult.PASS
    }
}