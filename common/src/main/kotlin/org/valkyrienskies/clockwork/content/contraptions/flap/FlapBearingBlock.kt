package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Consumer

class FlapBearingBlock(properties: Properties?) :
    BearingBlock(properties),
    IBE<FlapBearingBlockEntity> {
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
    }

    override fun getBlockEntityClass(): Class<FlapBearingBlockEntity> {
        return FlapBearingBlockEntity::class.java
    }

    override fun use(
        state: BlockState,
        worldIn: Level,
        pos: BlockPos,
        player: Player,
        handIn: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (!player.mayBuild()) return InteractionResult.FAIL
        if (player.isShiftKeyDown) return InteractionResult.FAIL
        if (player.getItemInHand(handIn).isEmpty) {
            if (!worldIn.isClientSide) {
                withBlockEntityDo(
                    worldIn,
                    pos,
                    Consumer withBlockEntityDo@{ te: FlapBearingBlockEntity ->
                        if (te.isRunning) {
                            te.disassemble()
                            return@withBlockEntityDo
                        }
                        te.assembleNextTick = true
                    }
                )
            }
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun getBlockEntityType(): BlockEntityType<out FlapBearingBlockEntity> {
        return ClockworkBlockEntities.FLAP_BEARING.get()
    }

    override fun newBlockEntity(p_153215_: BlockPos, p_153216_: BlockState): BlockEntity {
        val isSmart = this.descriptionId == "block.vs_clockwork.smart_flap_bearing"
        val maxSize = if (isSmart) -1L else 16L
        return FlapBearingBlockEntity(blockEntityType, p_153215_, p_153216_, maxSize, isSmart)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult {
        val resultType = super.onWrenched(state, context)
        if (!context.level.isClientSide && resultType.consumesAction()) withBlockEntityDo(
            context.level,
            context.clickedPos,
            FlapBearingBlockEntity::disassemble
        )
        return resultType
    }
}
