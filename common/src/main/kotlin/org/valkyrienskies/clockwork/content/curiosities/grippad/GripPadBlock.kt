package org.valkyrienskies.clockwork.content.curiosities.grippad

import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock
import net.createmod.catnip.placement.IPlacementHelper
import net.createmod.catnip.placement.PlacementHelpers
import net.createmod.catnip.placement.PlacementOffset
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.function.Predicate

class GripPadBlock(properties: Properties) : WrenchableDirectionalBlock(properties), IBE<GripPadBlockEntity> {
    init { registerDefaultState(defaultBlockState().setValue(POWERED, false)) }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder.add(POWERED))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState = defaultBlockState()
        .setValue(FACING, context.clickedFace)
        .setValue(POWERED, context.level.hasNeighborSignal(context.clickedPos))

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val stack = player.getItemInHand(hand)
        val helper = PlacementHelpers.get(placementHelperId)
        if (!player.isShiftKeyDown && player.mayBuild() && helper.matchesItem(stack)) {
            helper.getOffset(player, level, state, pos, hit).placeInWorld(level, stack.item as BlockItem, player, hand, hit)
            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, block: Block, from: BlockPos, moving: Boolean) {
        if (!level.isClientSide && state.getValue(POWERED) != level.hasNeighborSignal(pos))
            level.setBlock(pos, state.cycle(POWERED), 2)
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext) = SHAPES.getValue(state.getValue(FACING))
    override fun hasAnalogOutputSignal(state: BlockState) = true
    override fun getAnalogOutputSignal(state: BlockState, level: Level, pos: BlockPos) =
        if ((level.getBlockEntity(pos) as? GripPadBlockEntity)?.contact == true) 15 else 0

    override fun getBlockEntityClass() = GripPadBlockEntity::class.java
    override fun getBlockEntityType(): BlockEntityType<out GripPadBlockEntity> = ClockworkBlockEntities.GRIP_PAD.get()

    private class PlacementHelper : IPlacementHelper {
        override fun getItemPredicate(): Predicate<ItemStack> = Predicate { (it.item as? BlockItem)?.block is GripPadBlock }
        override fun getStatePredicate(): Predicate<BlockState> = Predicate { it.block is GripPadBlock }

        override fun getOffset(player: Player, world: Level, state: BlockState, pos: BlockPos, ray: BlockHitResult): PlacementOffset {
            val direction = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.location, state.getValue(FACING).axis) {
                world.getBlockState(pos.relative(it)).canBeReplaced()
            }.firstOrNull() ?: return PlacementOffset.fail()
            return PlacementOffset.success(pos.relative(direction)) { it.setValue(FACING, state.getValue(FACING)) }
        }
    }

    companion object {
        val POWERED = BlockStateProperties.POWERED
        private val placementHelperId = PlacementHelpers.register(PlacementHelper())
        private val SHAPES = mapOf(
            Direction.UP to box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0),
            Direction.DOWN to box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
            Direction.NORTH to box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0),
            Direction.SOUTH to box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0),
            Direction.WEST to box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            Direction.EAST to box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0)
        )
    }
}
