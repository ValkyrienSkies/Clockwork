package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems

class BladeControllerBlock(properties: Properties) : DirectionalBlock(properties), IBE<BladeControllerBlockEntity> {

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return defaultBlockState().setValue(FACING, context.nearestLookingDirection.opposite)
    }

    override fun getBlockEntityClass(): Class<BladeControllerBlockEntity> {
        return BladeControllerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out BladeControllerBlockEntity> {
        return ClockworkBlockEntities.BLADE_CONTROLLER.get()
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val blockEntity = level.getBlockEntity(pos) as BladeControllerBlockEntity?
            ?: return super.use(state, level, pos, player, hand, hit)

        if (player.getItemInHand(hand).`is`(ClockworkItems.PROPELLER_BLADE.get()) || player.getItemInHand(hand).`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())) {
            val success = blockEntity.insertBlade(player.getItemInHand(hand))
            if (success) {
                player.setItemInHand(hand, player.getItemInHand(hand).copy().also { it.shrink(1) })
                return InteractionResult.SUCCESS
            } else {
                return InteractionResult.FAIL
            }
        } else if (player.getItemInHand(hand).isEmpty) {
            val blade = blockEntity.removeBlade()
            if (!blade.isEmpty) {
                player.setItemInHand(hand, blade)
                return InteractionResult.SUCCESS
            } else {
                return InteractionResult.FAIL
            }
        } else {
            return super.use(state, level, pos, player, hand, hit)
        }
    }
}