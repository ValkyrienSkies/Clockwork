package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;

public abstract class ConnectedWingAlike extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public ConnectedWingAlike(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
        );
    }

    public static Direction getPreferredDirection(BlockPlaceContext context) {
        Axis preferredAxis = null;
        for (Direction side : Iterate.directions) {
            BlockState blockState = context.getLevel()
                    .getBlockState(context.getClickedPos()
                            .relative(side));
            if (blockState.getBlock() instanceof IRotate) {
                if (((IRotate) blockState.getBlock()).hasShaftTowards(context.getLevel(), context.getClickedPos()
                        .relative(side), blockState, side.getOpposite()))
                    if (preferredAxis != null && preferredAxis != side.getAxis()) {
                        preferredAxis = null;
                        break;
                    } else {
                        preferredAxis = side.getAxis();
                    }
            }
        }
        return preferredAxis == null ? null : switch (preferredAxis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.NORTH;

        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction preferredFacing = getPreferredDirection(context);
        if (preferredFacing != null && (context.getPlayer() == null || !context.getPlayer()
                .isShiftKeyDown()))
            return getNewState(this.defaultBlockState()
                    .setValue(FACING, preferredFacing), context.getLevel(), context.getClickedPos());
        return getNewState(this.defaultBlockState()
                .setValue(FACING, preferredFacing != null && context.getPlayer().isShiftKeyDown() ?
                        context.getClickedFace().getOpposite() : context.getNearestLookingDirection()), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState rotate(@NotNull BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(FACING)) {
                case NORTH -> state.setValue(FACING, Direction.EAST);
                case EAST -> state.setValue(FACING, Direction.UP);
                case UP -> state.setValue(FACING, Direction.NORTH);
                default -> state;
            };
            default -> state;
        };
    }

    public abstract BlockState getNewState(BlockState state, Level level, BlockPos pos);
}
