package org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor;

import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.fabric.AllClockworkTileEntities;

public class PropellorBearingBlock extends BearingBlock implements ITE<PropellorBearingTileEntity> {

    public static final EnumProperty<Direction> DIRECTION = EnumProperty.create("direction", Direction.class);
    public enum Direction implements StringRepresentable {
        PUSH, PULL, ;
        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
    public PropellorBearingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!player.mayBuild())
            return InteractionResult.FAIL;
        if (player.isShiftKeyDown())
            return InteractionResult.FAIL;
        if (player.getItemInHand(handIn)
                .isEmpty()) {
            if (worldIn.isClientSide) {
                withTileEntityDo(worldIn, pos, te -> {if (te.isRunning()) te.startSlowdown();});
                return InteractionResult.SUCCESS;
            }

            withTileEntityDo(worldIn, pos, te -> {
                if (te.isRunning() && !te.isOverStressed()) {
                    te.startSlowdown();
                    return;
                } else if (!te.isRunning() && !te.isOverStressed()) {
                    te.startSpinup();
                    return;
                }

            });
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Class<PropellorBearingTileEntity> getTileEntityClass() {
        return PropellorBearingTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends PropellorBearingTileEntity> getTileEntityType() {
        return AllClockworkTileEntities.PROPELLOR_BEARING.get();
    }

    public static Couple<Integer> getSpeedRange() {
        return Couple.create(1, 16);
    }

    public static PropellorBearingBlock.Direction getDirectionof(BlockState blockState) {
        return blockState.hasProperty(PropellorBearingBlock.DIRECTION) ? blockState.getValue(PropellorBearingBlock.DIRECTION) : Direction.PULL;

    }
}
