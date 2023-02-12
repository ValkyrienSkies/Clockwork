package org.valkyrienskies.clockwork.content.contraptions.ballooner;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.content.contraptions.wrench.WrenchItem;
import com.simibubi.create.foundation.block.ITE;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkBlocks;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.blocktype.IHeatableBlock;

import javax.annotation.Nullable;

import java.util.Random;

import static com.simibubi.create.content.contraptions.base.DirectionalKineticBlock.FACING;

public class BalloonerBlock extends HorizontalKineticBlock implements ITE<BalloonerBlockEntity>, IHeatableBlock {

//    public static final EnumProperty<EngineHeatLevel> HEAT_LEVEL = EnumProperty.create("blaze", EngineHeatLevel.class);

    public BalloonerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, EngineHeatLevel.SMOULDERING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HEAT_LEVEL);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ITE.super.newBlockEntity(pos, state);
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.of(1);
    }

    @Override
    public boolean hideStressImpact() {
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult blockRayTraceResult) {
        ItemStack heldItem = player.getItemInHand(hand);
        EngineHeatLevel heat = state.getValue(HEAT_LEVEL);
        BalloonerBlockEntity te = (BalloonerBlockEntity) world.getBlockEntity(pos);
        // FOR TESTING, DONT LEAVE IT AS A HAND DIPSHIT
        if (AllItems.BRASS_HAND.isIn(heldItem)) {
            if (te != null) {
                te.tryScan();
            }
        }
        boolean doNotConsume = player.isCreative();
        boolean forceOverflow = false;

        InteractionResultHolder<ItemStack> res =
                tryInsert(state, world, pos, heldItem, doNotConsume, forceOverflow, false);
        ItemStack leftover = res.getObject();
        if (!world.isClientSide && !doNotConsume && !leftover.isEmpty()) {
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, leftover);
            } else if (!player.getInventory()
                    .add(leftover)) {
                player.drop(leftover, false);
            }
        }

        return res.getResult() == InteractionResult.SUCCESS ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    public static InteractionResultHolder<ItemStack> tryInsert(BlockState state, Level world, BlockPos pos,
                                                               ItemStack stack, boolean doNotConsume, boolean forceOverflow, boolean simulate) {
        return PlatformUtils.tryInsert(state, world, pos, stack, doNotConsume, forceOverflow, simulate);
    }

    @Override
    public boolean showCapacityWithAnnotation() {
        return super.showCapacityWithAnnotation();
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return AllShapes.HEATER_BLOCK_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_220071_1_, BlockGetter p_220071_2_, BlockPos p_220071_3_,
                                        CollisionContext p_220071_4_) {
        if (p_220071_4_ == CollisionContext.empty())
            return AllShapes.HEATER_BLOCK_SPECIAL_COLLISION_SHAPE;
        return getShape(p_220071_1_, p_220071_2_, p_220071_3_, p_220071_4_);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Environment(EnvType.CLIENT)
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (random.nextInt(10) != 0)
            return;
        if (!state.getValue(HEAT_LEVEL)
                .isAtLeast(EngineHeatLevel.SMOULDERING))
            return;
        world.playLocalSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
                (double) ((float) pos.getZ() + 0.5F), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
    }

//    public static EngineHeatLevel getHeatLevelOf(BlockState blockState) {
//        return blockState.hasProperty(BalloonerBlock.HEAT_LEVEL) ? blockState.getValue(BalloonerBlock.HEAT_LEVEL)
//                : EngineHeatLevel.SMOULDERING;
//    }
//
//    public static int getLight(BlockState state) {
//        EngineHeatLevel level = state.getValue(HEAT_LEVEL);
//        return switch (level) {
//            case SMOULDERING -> 8;
//            default -> 15;
//        };
//    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState defaultState = defaultBlockState();
        return defaultState.setValue(HEAT_LEVEL, EngineHeatLevel.SMOULDERING)
                .setValue(HORIZONTAL_FACING, context.getHorizontalDirection()
                        .getOpposite());
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return super.onWrenched(state, context);
    }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }
    @Override
    public Class<BalloonerBlockEntity> getTileEntityClass() {
        return BalloonerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends BalloonerBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.BALLOONER.get();
    }
}

