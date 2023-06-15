package org.valkyrienskies.clockwork.util.blocktype;

import com.simibubi.create.content.contraptions.base.IRotate;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkShapes;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.content.physicalities.motion.wing.WingBlockItem;

import java.util.List;

public abstract class ConnectedWingAlike extends Block implements ITE<ColorBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public ConnectedWingAlike(Properties properties) {
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

    @Override
    public VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return ClockWorkShapes.WING.get(switch (pState.getValue(FACING)) {
            case EAST, WEST -> Axis.X;
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
        });
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        level.setBlockAndUpdate(pos, getNewState(state, level, pos));
    }

    @Override
    public InteractionResult use(BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        ColorBlockEntity be = (ColorBlockEntity) level.getBlockEntity(pos);
        assert be != null;
        int color = be.getColor();

        if (stack.getItem() instanceof DyeItem dye && color != dye.getDyeColor().getTextColor()) {
            be.setColor(color == -1 ? dye.getDyeColor().getTextColor() :
                    Color.mixColors(color, dye.getDyeColor().getTextColor(), 0.5f));

            if (!level.isClientSide && !player.isCreative()) {
                if (stack.getCount() > 1)
                    stack.shrink(1);
                else if (stack.getCount() == 1)
                    player.setItemInHand(hand, ItemStack.EMPTY);
            }

            return InteractionResult.SUCCESS;
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<ColorBlockEntity> getTileEntityClass() {
        return ColorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ColorBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.COLOR_BLOCK_ENTITY.get();
    }

    @Override
    public List<ItemStack> getDrops(@NotNull BlockState state, LootContext.@NotNull Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);

        drops.replaceAll(stack -> {
            ColorBlockEntity be = (ColorBlockEntity) builder.getParameter(LootContextParams.BLOCK_ENTITY);
            int color = be.getColor();
            if ((stack.getItem() instanceof WingBlockItem) && color != -1)
                stack.getOrCreateTag().putInt("Clockwork$color", color);
            return stack;
        });

        return drops;
    }



    @Override
    public ItemStack getCloneItemStack(@NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        ColorBlockEntity be = (ColorBlockEntity) level.getBlockEntity(pos);
        assert be != null;
        int color = be.getColor();

        if (color != -1) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putInt("Clockwork$color", color);
        }

        return stack;
    }


//    @Nullable
//    @Override
//    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
//        return new ColorBlockEntity(ClockWorkBlockEntities.WING.get(), pos, state);
//    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
}
