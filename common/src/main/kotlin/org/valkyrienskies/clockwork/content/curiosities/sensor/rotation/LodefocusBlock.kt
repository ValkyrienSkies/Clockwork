package org.valkyrienskies.clockwork.content.curiosities.sensor.rotation

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.CompassItem
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.BlockHitResult

class LodefocusBlock(properties: Properties) : Block(properties), IBE<LodefocusBlockEntity> {
    override fun getPistonPushReaction(state: BlockState): PushReaction {
        return PushReaction.NORMAL
    }

    override fun getBlockEntityClass(): Class<LodefocusBlockEntity> {
        return LodefocusBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out LodefocusBlockEntity> {
        TODO("Not yet implemented")
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            val item = player.getItemInHand(hand)
            if (item.`is`(Items.COMPASS) && item.tag != null && item.tag!!.getBoolean(CompassItem.TAG_LODESTONE_TRACKED)) {
                val blockEntity = level.getBlockEntity(pos)
                if (blockEntity is LodefocusBlockEntity) {
                    blockEntity.setItem(0, item.copy())
                    item.shrink(1)
                    return InteractionResult.SUCCESS
                }
            }
            return super.use(state, level, pos, player, hand, hit)
        }
        return super.use(state, level, pos, player, hand, hit)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!isMoving) {
            withBlockEntityDo(level, pos) { blockEntity ->
                blockEntity.dropCompass()
            }
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }
}