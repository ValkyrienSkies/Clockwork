package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class DuctTankItem(block: Block, properties: Properties) : BlockItem(block, properties) {

    override fun place(ctx: BlockPlaceContext): InteractionResult {
        val initialResult = super.place(ctx)
        if (!initialResult.consumesAction()) return initialResult
        tryMultiPlace(ctx)
        return initialResult
    }

    private fun isTank(state: BlockState): Boolean = state.block is DuctTankBlock

    private fun tryMultiPlace(ctx: BlockPlaceContext) {
        val player = ctx.player ?: return
        if (player.isShiftKeyDown) return
        val face = ctx.clickedFace
        if (!face.axis.isVertical) return

        val stack = ctx.itemInHand
        val level = ctx.level
        val pos = ctx.clickedPos
        val placedOnPos = pos.relative(face.opposite)
        if (!isTank(level.getBlockState(placedOnPos))) return
        if (SymmetryWandItem.presentInHotbar(player)) return

        val tankAt = level.getBlockEntity(placedOnPos) as? DuctTankBlockEntity ?: return
        val controller = tankAt.getControllerBE<DuctTankBlockEntity>() ?: return

        val width = controller.width
        if (width <= 1) return

        val startPos: BlockPos = if (face == Direction.DOWN) controller.blockPos.below()
        else controller.blockPos.above(controller.height)
        if (startPos.y != pos.y) return

        var tanksToPlace = 0
        for (xOffset in 0 until width) {
            for (zOffset in 0 until width) {
                val state = level.getBlockState(startPos.offset(xOffset, 0, zOffset))
                if (isTank(state)) continue
                if (!state.canBeReplaced()) return
                tanksToPlace++
            }
        }
        if (!player.isCreative && stack.count < tanksToPlace) return

        for (xOffset in 0 until width) {
            for (zOffset in 0 until width) {
                val offsetPos = startPos.offset(xOffset, 0, zOffset)
                if (isTank(level.getBlockState(offsetPos))) continue
                super.place(BlockPlaceContext.at(ctx, offsetPos, face))
            }
        }
    }
}
