package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkItems

/** The muzzle faces away from the single shaft input. */
class HarpoonBlock(properties: Properties) : DirectionalKineticBlock(properties), IBE<HarpoonBlockEntity> {
    init { registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(POWERED))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        // Match shaft axis selection: align with nearby shafts, use the clicked axis when
        // sneaking past that preference, otherwise use the player's nearest look axis.
        val preferred = RotatedPillarKineticBlock.getPreferredAxis(context)
        val sneaking = context.player?.isShiftKeyDown == true
        val facing = when {
            preferred != null && !sneaking -> getPreferredFacing(context)!!.opposite
            preferred != null -> context.clickedFace
            else -> context.nearestLookingDirection.opposite
        }
        return defaultBlockState().setValue(FACING, facing)
            .setValue(POWERED, context.level.hasNeighborSignal(context.clickedPos))
    }

    override fun getRotationAxis(state: BlockState) = state.getValue(FACING).axis
    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction) =
        face == state.getValue(FACING).opposite

    override fun getBlockEntityClass() = HarpoonBlockEntity::class.java
    override fun getBlockEntityType() = ClockworkBlockEntities.HARPOON.get()

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, from: BlockPos, moving: Boolean) {
        if (!level.isClientSide) (level.getBlockEntity(pos) as? HarpoonBlockEntity)?.redstoneChanged()
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand,
                     hit: BlockHitResult): InteractionResult {
        if (!player.mayBuild() || player.isSpectator) return InteractionResult.PASS
        val launcher = level.getBlockEntity(pos) as? HarpoonBlockEntity ?: return InteractionResult.PASS
        val held = player.getItemInHand(hand)
        if (held.`is`(ClockworkItems.HARPOON.get()) && launcher.loadedHarpoon.isEmpty) {
            if (!level.isClientSide) {
                launcher.load(held)
                if (!player.abilities.instabuild) held.shrink(1)
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        if (player.isShiftKeyDown && held.isEmpty && !launcher.loadedHarpoon.isEmpty) {
            if (!level.isClientSide) {
                val removed = launcher.unload()
                if (!player.addItem(removed)) player.drop(removed, false)
            }
            return InteractionResult.sidedSuccess(level.isClientSide)
        }
        return InteractionResult.PASS
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, next: BlockState, moved: Boolean) {
        if (!state.`is`(next.block) && !level.isClientSide) {
            val launcher = level.getBlockEntity(pos) as? HarpoonBlockEntity
            if (launcher != null) {
                launcher.release()
                popResource(level, pos, launcher.unload())
            }
        }
        IBE.onRemove(state, level, pos, next)
        super.onRemove(state, level, pos, next, moved)
    }

    companion object { val POWERED = BlockStateProperties.POWERED }
}
