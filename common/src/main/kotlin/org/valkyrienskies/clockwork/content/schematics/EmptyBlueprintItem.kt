package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos

class EmptyBlueprintItem(properties: Properties) : CWItem(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.PASS
        if (context.hand != InteractionHand.MAIN_HAND || !player.mayBuild()) return InteractionResult.PASS
        val ship = context.level.getShipManagingPos(context.clickedPos) ?: return InteractionResult.PASS
        if (context.level.isClientSide) VdexBlueprintClient.openCapture()
        else if (ship is ServerShip) VdexBlueprintLibrary.beginCapture(player as ServerPlayer, ship)
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(player.getItemInHand(hand))
        if (level.isClientSide) VdexBlueprintClient.open()
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide)
    }
}
