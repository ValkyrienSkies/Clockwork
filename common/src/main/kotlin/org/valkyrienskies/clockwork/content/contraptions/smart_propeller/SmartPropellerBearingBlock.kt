package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities


class SmartPropellerBearingBlock(properties: Properties?) : BearingBlock(properties),
    IBE<SmartPropellerBearingBlockEntity> {

    override fun getBlockEntityClass(): Class<SmartPropellerBearingBlockEntity> {
        return SmartPropellerBearingBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SmartPropellerBearingBlockEntity> {
        return ClockworkBlockEntities.SMART_PROPELLER_BEARING.get()
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val player = context.player
        if (player != null) {
            val pitch = player.xRot

            val facing = if (pitch < 0) {
                Direction.DOWN
            } else {
                Direction.UP
            }

            return defaultBlockState().setValue(FACING, facing)
        }

        return defaultBlockState().setValue(FACING, Direction.UP)
    }


    override fun use(state: BlockState,
                     worldIn: Level,
                     pos: BlockPos,
                     player: Player,
                     handIn: InteractionHand,
                     hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL

        if (player.getItemInHand(handIn).isEmpty) {
            if (worldIn.isClientSide) {
                withBlockEntityDo(worldIn, pos) { te -> if (te.isRunning) te.startDisassemblySlowdown() }

                return InteractionResult.SUCCESS
            }

            withBlockEntityDo(worldIn, pos) { te ->
                if (te.isRunning) {
                    te.startDisassemblySlowdown()
                    return@withBlockEntityDo
                }
                te.setAssembleNextTick(true)
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }
}