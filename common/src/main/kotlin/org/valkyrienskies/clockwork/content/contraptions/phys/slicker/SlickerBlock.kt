package org.valkyrienskies.clockwork.content.contraptions.phys.slicker

import com.simibubi.create.content.contraptions.chassis.StickerBlock
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class SlickerBlock(properties: Properties) : WrenchableDirectionalBlock(properties.noOcclusion()), IBE<SlickerBlockEntity> {

    companion object {
        val POWERED: BooleanProperty = BooleanProperty.create("powered")
        val EXTENDED: BooleanProperty = BooleanProperty.create("extended")
    }

    init {
        registerDefaultState(stateDefinition.any()
            .setValue(POWERED, false)
            .setValue(EXTENDED, false)
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        val nearestLookingDirection = context.nearestLookingDirection
        val shouldPower = context.level.hasNeighborSignal(context.clickedPos)
        val facing = if (context.player != null && context.player!!.isShiftKeyDown) nearestLookingDirection else nearestLookingDirection.opposite

        return defaultBlockState().setValue(FACING, facing)
            .setValue(POWERED, shouldPower)
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(POWERED, EXTENDED)
        super.createBlockStateDefinition(builder)
    }

    override fun neighborChanged(
        state: BlockState, worldIn: Level, pos: BlockPos, blockIn: Block, fromPos: BlockPos,
        isMoving: Boolean
    ) {

        val previouslyPowered = state.getValue(StickerBlock.POWERED)
        if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
            var state2 = state.cycle(StickerBlock.POWERED)
            if (state2.getValue(StickerBlock.POWERED)) state2 = state2.cycle(StickerBlock.EXTENDED)
            worldIn.setBlock(pos, state2, 2)
        }
    }

    override fun getBlockEntityClass(): Class<SlickerBlockEntity> {
        return SlickerBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SlickerBlockEntity> {
        return ClockworkBlockEntities.SLICKER.get()
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pIsMoving: Boolean
    ) {
        IBE.onRemove(pState, pLevel, pPos, pNewState)
    }
}
