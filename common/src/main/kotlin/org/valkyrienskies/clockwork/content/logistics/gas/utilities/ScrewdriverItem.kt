package org.valkyrienskies.clockwork.content.logistics.gas.utilities

import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import org.valkyrienskies.clockwork.content.logistics.gas.IScrewdrivable

class ScrewdriverItem(properties: Properties) : Item(properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player
        if (player == null || !player.mayBuild()) return super.useOn(context)
        val state = context.level
            .getBlockState(context.clickedPos)
        val block = state.block
        if (block is IScrewdrivable) {
            return super.useOn(context)
        }
        val actor = block as IScrewdrivable
        return if (player.isShiftKeyDown) actor.onSneakScrewdrived(state, context) else actor.onScrewdrived(state, context)
    }
}