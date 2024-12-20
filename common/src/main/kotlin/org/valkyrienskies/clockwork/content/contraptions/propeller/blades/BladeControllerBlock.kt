package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkTags

class BladeControllerBlock(properties: Properties) : DirectionalBlock(properties), IBE<BladeControllerBlockEntity> {

    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.UP)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
        super.createBlockStateDefinition(builder)
    }

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

        if (level.isClientSide) {
            return super.use(state, level, pos, player, hand, hit)
        }

        if (player.mainHandItem.`is`(ClockworkItems.PROPELLER_BLADE.get()) || player.mainHandItem.`is`(ClockworkItems.WIDE_PROPELLER_BLADE.get())) {
            if (blockEntity.bladeCooldown > 0) {
                return InteractionResult.FAIL
            }
            val success = blockEntity.insertBlade(player.mainHandItem.copy())
            if (success) {
                if (!player.isCreative) player.setItemInHand(hand, player.getItemInHand(InteractionHand.MAIN_HAND).copy().also { it.shrink(1) })
                return InteractionResult.SUCCESS
            } else {
                return InteractionResult.FAIL
            }
        } else if (player.mainHandItem.isEmpty) {
            if (blockEntity.bladeCooldown > 0) {
                return InteractionResult.FAIL
            }
            val blade = blockEntity.removeBlade()
            if (!blade.isEmpty) {
                player.setItemInHand(InteractionHand.MAIN_HAND, blade)
                return InteractionResult.SUCCESS
            } else {
                return InteractionResult.FAIL
            }
        } else {
            return super.use(state, level, pos, player, hand, hit)
        }
    }
}