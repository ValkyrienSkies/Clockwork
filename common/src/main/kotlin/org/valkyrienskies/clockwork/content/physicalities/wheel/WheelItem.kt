package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext

class WheelItem(properties: Properties, val size: WheelSize) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val hub = context.level.getBlockEntity(context.clickedPos) as? WheelHubBlockEntity ?: return InteractionResult.PASS
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild() || hub.wheel != null || context.clickedFace != hub.facing) return InteractionResult.PASS
        if (context.level.isClientSide) return InteractionResult.SUCCESS
        if (!hub.install(size, player, context.itemInHand.tag)) return InteractionResult.FAIL
        if (!player.isCreative) context.itemInHand.shrink(1)
        return InteractionResult.CONSUME
    }
}
