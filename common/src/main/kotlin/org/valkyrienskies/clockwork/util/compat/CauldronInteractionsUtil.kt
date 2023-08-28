package org.valkyrienskies.clockwork.util.compat

import net.minecraft.core.BlockPos
import net.minecraft.core.cauldron.CauldronInteraction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LayeredCauldronBlock
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem

object CauldronInteractionsUtil {
    var DYED_WING =
        CauldronInteraction { state: BlockState?, world: Level, pos: BlockPos?, player: Player?, hand: InteractionHand?, stack: ItemStack ->
            val item = stack.item
            if (item !is DyedWingBlockItem) {
                return@CauldronInteraction InteractionResult.PASS
            } else if (!item.hasColor(stack)) {
                return@CauldronInteraction InteractionResult.PASS
            } else {
                if (!world.isClientSide) {
                    item.clearColor(stack)
                    LayeredCauldronBlock.lowerFillLevel(state, world, pos)
                }
                return@CauldronInteraction InteractionResult.sidedSuccess(world.isClientSide)
            }
        }
}