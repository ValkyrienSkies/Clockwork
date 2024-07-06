package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals

class DeliveryChuteBlock(properties: Properties): Block(properties), IBE<DeliveryChuteBlockEntity> {
    override fun getBlockEntityClass(): Class<DeliveryChuteBlockEntity> {

        return DeliveryChuteBlockEntity::class.java
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {

        return FrequencySlotGlobals.use(state, level, pos, player, hand, hit)
    }

    override fun getBlockEntityType(): BlockEntityType<out DeliveryChuteBlockEntity> {
        return ClockworkBlockEntities.DELIVERY_CHUTE.get()

    }


    override fun canSurvive(state: BlockState?, level: LevelReader, pos: BlockPos): Boolean {
        // This is a really stupid way to do it, but neither == ALlBlocks.Depot nor anything else seems to work
        val desc = level.getBlockState(pos.below()).block.descriptionId
        return desc == "block.create.depot" || desc == "block.create.belt" || desc == "block.create.chute"
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState?,
        level: LevelAccessor?,
        currentPos: BlockPos?,
        neighborPos: BlockPos?
    ): BlockState {
        if (level == null || currentPos == null || !canSurvive(state,level,currentPos))  {
            return Blocks.AIR.defaultBlockState()
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos)
    }

}
