package org.valkyrienskies.clockwork.content.propulsion.afterblazer;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.platform.PlatformUtils;
import org.valkyrienskies.clockwork.platform.SharedValues;
import org.valkyrienskies.clockwork.util.blocktype.LiquidFuelType;

import java.util.function.Function;

public class AfterblazerBlock extends DirectionalBlock implements IBE<AfterblazerEngineBlockEntity>, IWrenchable {
    public AfterblazerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);

        LiquidFuelType fuelType = PlatformUtils.getLiquidFuelTypeFromItemStack(held);
        if (fuelType.isAtLeast(LiquidFuelType.STALE)) {
            final boolean[] success = {false};
            withBlockEntityDo(level, pos, te -> {
                success[0] = te.tryAddBucket(fuelType);
                te.setChanged();
            });
            if (!player.isCreative() && success[0]) {
                held.shrink(1);
                player.addItem(new ItemStack(Items.BUCKET));
                ClockWorkSounds.THICK_FLUID_EMPTY.playAt(level, new Vec3(pos.getX(), pos.getY(), pos.getZ()), 1f, 1f, false);
                return InteractionResult.PASS;
            } else if (player.isCreative() && success[0]) {
                return InteractionResult.PASS;
            } else {
                return InteractionResult.FAIL;
            }

        }

        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public Class<AfterblazerEngineBlockEntity> getBlockEntityClass() {
        return AfterblazerEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AfterblazerEngineBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.AFTERBLAZER.get();
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                  boolean isMoving) {
        if (worldIn.isClientSide) {
            return;
        }
        withBlockEntityDo(worldIn, pos, te -> {
            te.getPower(worldIn, pos);
            te.setChanged();
        });
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState defaultState = defaultBlockState();
        return defaultState.setValue(FACING, context.getNearestLookingDirection()
                .getOpposite());
    }

}
