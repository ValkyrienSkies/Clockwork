package org.valkyrienskies.clockwork.content.contraptions.afterblazer;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import io.github.fabricators_of_create.porting_lib.block.ConnectableRedstoneBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkItems;
import org.valkyrienskies.clockwork.ClockWorkShapes;

import javax.annotation.Nullable;
import java.util.Random;

public class AfterblazerBlock extends DirectionalBlock implements ITE<AfterblazerBlockEntity>, IWrenchable {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<EngineHeatLevel> HEAT_LEVEL = EnumProperty.create("afterblazer", EngineHeatLevel.class);

    public AfterblazerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HEAT_LEVEL, EngineHeatLevel.SMOULDERING).setValue(POWERED, false));
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
        builder.add(HEAT_LEVEL, FACING);
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
        EngineHeatLevel heat = state.getValue(HEAT_LEVEL);

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
        EngineHeatLevel initialHeat = EngineHeatLevel.SMOULDERING;
        return defaultState.setValue(HEAT_LEVEL, initialHeat)
                .setValue(FACING, context.getNearestLookingDirection()
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
        if (!state.getValue(HEAT_LEVEL)
                .isAtLeast(EngineHeatLevel.SMOULDERING))
            return;

        Direction dir = state.getValue(FACING);
        double x = pos.getX() + ((dir.getStepX() + 1) * 0.5);
        double y = pos.getY() + ((dir.getStepY() + 1) * 0.5);
        double z = pos.getZ() + ((dir.getStepZ() + 1) * 0.5);
        AfterblazerBlockEntity te = getTileEntity(world, pos);
        double force = 0;
        if (te != null) {
            force = te.getParticleThrust(getHeatLevelOf(state));
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

    public static EngineHeatLevel getHeatLevelOf(BlockState blockState) {
        return blockState.hasProperty(AfterblazerBlock.HEAT_LEVEL) ? blockState.getValue(AfterblazerBlock.HEAT_LEVEL)
                : EngineHeatLevel.SMOULDERING;
    }

    public static int getLight(BlockState state) {
        EngineHeatLevel level = state.getValue(HEAT_LEVEL);
        return switch (level) {
            case SMOULDERING -> 8;
            case INFURIATED -> 20;
            default -> 15;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return ClockWorkShapes.AFTERBLAZER.get(state.getValue(FACING));
    }



    public enum EngineHeatLevel implements StringRepresentable {
        SMOULDERING, FADING, KINDLED, SEETHING, INFURIATED;

        public static EngineHeatLevel byIndex(int index) {
            return values()[index];
        }

        public EngineHeatLevel nextActiveLevel() {
            return byIndex(ordinal() % (values().length - 1) + 1);
        }

        public boolean isAtLeast(EngineHeatLevel heatLevel) {
            return this.ordinal() >= heatLevel.ordinal();
        }

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
