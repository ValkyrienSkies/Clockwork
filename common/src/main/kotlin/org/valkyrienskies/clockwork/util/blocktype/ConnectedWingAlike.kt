package org.valkyrienskies.clockwork.util.blocktype

import com.simibubi.create.content.kinetics.base.IRotate
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.placement.IPlacementHelper
import net.createmod.catnip.placement.PlacementHelpers
import net.createmod.catnip.placement.PlacementOffset
import net.minecraft.MethodsReturnNonnullByDefault
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
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkShapes
import org.valkyrienskies.clockwork.content.generic.ColorBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.wing.DyedWingBlockItem
import java.util.function.Predicate
import kotlin.collections.getValue

abstract class ConnectedWingAlike(properties: Properties?) : Block(properties) {
    init {
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        )
    }



    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val state = super.getStateForPlacement(context);
        return getNewState(state?.setValue(FACING, context.clickedFace
            .getOpposite()), context.level, context.clickedPos)

//        val preferredFacing = getPreferredDirection(context)
//        return if (preferredFacing != null && (context.player == null || !context.player!!.isShiftKeyDown)) {
//            getNewState(
//                defaultBlockState().setValue(FACING, preferredFacing), context.level, context.clickedPos
//            )
//        } else {
//            getNewState(
//                defaultBlockState().setValue(
//                    FACING,
//                    if (preferredFacing != null && context.player!!.isShiftKeyDown) context.clickedFace.opposite else context.nearestLookingDirection
//                ),
//                context.level,
//                context.clickedPos
//            )
//        }
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        val heldItem: ItemStack = player.getItemInHand(hand)

        val placementHelper: IPlacementHelper = PlacementHelpers.get(placementHelperId);
        if (!player.isShiftKeyDown() && player.mayBuild()) {
            if (placementHelper.matchesItem(heldItem)) {
                var color = -1
                if (heldItem.item is DyedWingBlockItem) {
                    color = if (heldItem.hasTag() && heldItem.getOrCreateTag().contains("Clockwork\$color")) heldItem.getOrCreateTag()
                        .getInt("Clockwork\$color") else -1
                }
                val offset = placementHelper.getOffset(player, level, state, pos, hit)
                val result = offset.placeInWorld(level, (heldItem.item) as BlockItem, player, hand, hit)

                if (result != InteractionResult.SUCCESS) {
                    return result
                }

                if (level.getBlockEntity(offset.blockPos) is ColorBlockEntity) {
                    val be: ColorBlockEntity = (level.getBlockEntity(offset.blockPos) as ColorBlockEntity)
                    be.setColor(color)
                }

                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    override fun rotate(state: BlockState, rot: Rotation): BlockState {
        return when (rot) {
            Rotation.COUNTERCLOCKWISE_90, Rotation.CLOCKWISE_90 -> when (state.getValue(FACING)) {
                Direction.NORTH -> state.setValue(FACING, Direction.EAST)
                Direction.EAST -> state.setValue(FACING, Direction.UP)
                Direction.UP -> state.setValue(FACING, Direction.NORTH)
                else -> state
            }

            else -> state
        }
    }

    abstract fun getNewState(state: BlockState?, level: Level?, pos: BlockPos?): BlockState?
    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return ClockworkShapes.WING.get(
            when (pState.getValue<Direction>(FACING)) {
                Direction.EAST, Direction.WEST -> Direction.Axis.X
                Direction.UP, Direction.DOWN -> Direction.Axis.Y
                Direction.NORTH, Direction.SOUTH -> Direction.Axis.Z
            }
        )
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        level.setBlockAndUpdate(pos, getNewState(state, level, pos))
    }

    @MethodsReturnNonnullByDefault
    private class PlacementHelper : IPlacementHelper {

        override fun getItemPredicate(): Predicate<ItemStack> {
            return Predicate { i -> ClockworkBlocks.WING.isIn(i) || ClockworkBlocks.FLAP.isIn(i) }
        }

        override fun getStatePredicate(): Predicate<BlockState> {
            return Predicate { state: BlockState -> state.block is ConnectedWingAlike } //kotlin why
        }

        override fun getOffset(
            player: Player,
            world: Level,
            state: BlockState,
            pos: BlockPos,
            ray: BlockHitResult
        ): PlacementOffset {
            val directions: List<Direction> = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(),
            state.getValue(FACING)
                .getAxis()) {dir -> world.getBlockState(pos.relative(dir)).canBeReplaced()}

            if (directions.isEmpty())
                return PlacementOffset.fail();
            else {
                return PlacementOffset.success(pos.relative(directions.get(0)),
                    {s -> s.setValue(FACING, state.getValue(FACING))})
            }
        }
    }

    companion object {
        val FACING = BlockStateProperties.FACING
        val NORTH = BlockStateProperties.NORTH
        val SOUTH = BlockStateProperties.SOUTH
        val EAST = BlockStateProperties.EAST
        val WEST = BlockStateProperties.WEST
        val UP = BlockStateProperties.UP
        val DOWN = BlockStateProperties.DOWN

        @JvmStatic
        val placementHelperId: Int = PlacementHelpers.register(PlacementHelper())

        fun getPreferredDirection(context: BlockPlaceContext): Direction? {
            var preferredAxis: Direction.Axis? = null
            for (side in Iterate.directions) {
                val blockState = context.level
                    .getBlockState(
                        context.clickedPos
                            .relative(side)
                    )
                if (blockState.block is ConnectedWingAlike && !context.isSecondaryUseActive) {
                    if ((blockState.block as ConnectedWingAlike).defaultBlockState()
                            .getValue(FACING)
                            .axis == side.axis
                    ) if (preferredAxis != null && preferredAxis !== side.axis) {
                        preferredAxis = null
                        break
                    } else {
                        preferredAxis = side.axis
                    }
                }
            }
            return if (preferredAxis == null) null else when (preferredAxis) {
                Direction.Axis.X -> Direction.EAST
                Direction.Axis.Y -> Direction.UP
                Direction.Axis.Z -> Direction.NORTH
            }
        }
    }
}
