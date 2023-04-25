package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkShapes;
import org.valkyrienskies.clockwork.util.blocktype.EngineHeatLevel;
import org.valkyrienskies.clockwork.util.blocktype.IHeatableBlock;

import javax.annotation.Nullable;
import java.util.Random;

public class AfterblazerBlock extends DirectionalBlock implements ITE<AfterblazerBlockEntity>, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
//    public static final EnumProperty<EngineHeatLevel> HEAT_LEVEL = EnumProperty.create("afterblazer", EngineHeatLevel.class);

    public AfterblazerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    public Class<AfterblazerBlockEntity> getTileEntityClass() {
        return AfterblazerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AfterblazerBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.AFTERBLAZER.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }



    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ITE.super.newBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult blockRayTraceResult) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (AllItems.GOGGLES.isIn(heldItem))
            return onTileEntityUse(world, pos, bbte -> {
                if (bbte.goggles)
                    return InteractionResult.PASS;
                bbte.goggles = true;
                bbte.notifyUpdate();
                return InteractionResult.SUCCESS;
            });
        boolean doNotConsume = player.isCreative();
        if (ClockWorkItems.STRATODONUT.isIn(heldItem))
            return onTileEntityUse(world, pos, bbte -> {
                if (bbte.pissedOff)
                    return InteractionResult.PASS;
                bbte.pissedOff = true;
                bbte.notifyUpdate();

                if (!doNotConsume) {
                    heldItem.shrink(1);
                }
                return InteractionResult.SUCCESS;
            });

        if (heldItem.isEmpty())
            return onTileEntityUse(world, pos, bbte -> {
                if (!bbte.goggles)
                    return InteractionResult.PASS;
                bbte.goggles = false;
                bbte.notifyUpdate();
                return InteractionResult.SUCCESS;
            });

        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState defaultState = defaultBlockState();
        return defaultState.setValue(FACING, context.getNearestLookingDirection()
                        .getOpposite())
                .setValue(POWERED, false);
    }



    @Override
    public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Environment(EnvType.CLIENT)
    public void animateTick(BlockState state, Level world, BlockPos pos, Random random) {
        if (random.nextInt(10) != 0)
            return;

        Direction dir = state.getValue(FACING);
        double x = pos.getX() + ((dir.getStepX() + 1) * 0.5);
        double y = pos.getY() + ((dir.getStepY() + 1) * 0.5);
        double z = pos.getZ() + ((dir.getStepZ() + 1) * 0.5);
        AfterblazerBlockEntity te = getTileEntity(world, pos);
        double force = 0;
        if (te != null) {
            force = te.getParticleThrust();
        }
        double speedX = dir.getStepX() * force;
        double speedY = dir.getStepY() * force;
        double speedZ = dir.getStepZ() * force;

        for (int i = 0; i < 16; i++) {
            double x2 = random.nextDouble() * 0.2 - 1;
            double y2 = random.nextDouble() * 0.2 - 1;
            double z2 = random.nextDouble() * 0.2 - 1;
            world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x2, y2, z2, speedX, speedY, speedZ);
        }

        world.playLocalSound((double) ((float) pos.getX() + 0.5F), (double) ((float) pos.getY() + 0.5F),
                (double) ((float) pos.getZ() + 0.5F), SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                0.2F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
    }


    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return ClockWorkShapes.AFTERBLAZER.get(state.getValue(FACING));
    }
}
