package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class TriAxisBlockWithConnections extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty connectedOne = BooleanProperty.create("connectedone");
    public static final BooleanProperty connectedTwo = BooleanProperty.create("connectedtwo");
    public static final BooleanProperty connectedThree = BooleanProperty.create("connectedthree");
    public static final BooleanProperty connectedFour = BooleanProperty.create("connectedfour");

    public TriAxisBlockWithConnections(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(defaultBlockState()
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(connectedOne, false)
                .setValue(connectedTwo, false)
                .setValue(connectedThree, false)
                .setValue(connectedFour, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
        builder.add(connectedOne);
        builder.add(connectedTwo);
        builder.add(connectedThree);
        builder.add(connectedFour);
        super.createBlockStateDefinition(builder);
    }

    public static Axis getPreferredAxis(BlockPlaceContext context) {
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
        return preferredAxis;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Axis preferredAxis = getPreferredAxis(context);
        if (preferredAxis != null && (context.getPlayer() == null || !context.getPlayer()
                .isShiftKeyDown()))
            return this.defaultBlockState()
                    .setValue(AXIS, preferredAxis);
        return this.defaultBlockState()
                .setValue(AXIS, preferredAxis != null && context.getPlayer()
                        .isShiftKeyDown() ? context.getClickedFace()
                        .getAxis()
                        : context.getNearestLookingDirection()
                        .getAxis());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        switch (rot) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch (state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }
}
