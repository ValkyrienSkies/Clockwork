package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class PhysicalGantryBlock(properties: Properties) : DirectionalAxisKineticBlock(properties), IBE<PhysicalGantryBlockEntity> {
    init {
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(ASSEMBLED))
    }

    override fun getFacingForPlacement(context: BlockPlaceContext): Direction = context.clickedFace

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val state = super.getStateForPlacement(context) ?: return null
        val shaft = context.level.getBlockState(context.clickedPos.relative(context.clickedFace.opposite))
        if (!AllBlocks.GANTRY_SHAFT.has(shaft)) return state
        return if (getRotationAxis(state) == shaft.getValue(GantryShaftBlock.FACING).axis)
            state.cycle(AXIS_ALONG_FIRST_COORDINATE) else state
    }

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        if (state.getValue(ASSEMBLED)) return true
        val facing = state.getValue(FACING)
        val shaft = level.getBlockState(pos.relative(facing.opposite))
        return AllBlocks.GANTRY_SHAFT.has(shaft) && shaft.getValue(GantryShaftBlock.FACING).axis != facing.axis
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || player.isShiftKeyDown || hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty)
            return InteractionResult.PASS
        if (!level.isClientSide) withBlockEntityDo(level, pos, PhysicalGantryBlockEntity::toggleAssembly)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onWrenched(state: BlockState, context: UseOnContext): InteractionResult =
        if (state.getValue(ASSEMBLED)) InteractionResult.PASS else super.onWrenched(state, context)

    override fun getBlockEntityClass() = PhysicalGantryBlockEntity::class.java

    override fun getBlockEntityType() = ClockworkBlockEntities.PHYS_GANTRY.get()

    companion object {
        val ASSEMBLED: BooleanProperty = BooleanProperty.create("assembled")
    }
}
