package org.valkyrienskies.clockwork.content.contraptions.propellor;

import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class PropellorBearingBlock extends BearingBlock implements IBE<PropellorBearingBlockEntity> {

    public static final EnumProperty<Direction> DIRECTION = EnumProperty.create("direction", Direction.class);

    public PropellorBearingBlock(Properties properties) {
        super(properties);
    }

    public static Couple<Integer> getSpeedRange() {
        return Couple.create(1, 16);
    }

    public static PropellorBearingBlock.Direction getDirectionof(BlockState blockState) {
        return blockState.hasProperty(PropellorBearingBlock.DIRECTION) ? blockState.getValue(PropellorBearingBlock.DIRECTION) : Direction.PULL;

    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DIRECTION);
        super.createBlockStateDefinition(builder);
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
            if (!worldIn.isClientSide) {
                withBlockEntityDo(worldIn, pos, te -> {
                    if (te.running) {
                        te.shutDown();
                        return;
                    }
                    te.assembleNextTick = true;
                });
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public Class<PropellorBearingBlockEntity> getBlockEntityClass() {
        return PropellorBearingBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PropellorBearingBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.PROPELLOR_BEARING.get();
    }
    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, net.minecraft.core.Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }
    @Override
    public net.minecraft.core.Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    public enum Direction implements StringRepresentable {
        PUSH, PULL,
        ;

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
