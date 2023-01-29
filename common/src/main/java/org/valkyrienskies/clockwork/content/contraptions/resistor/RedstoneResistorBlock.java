package org.valkyrienskies.clockwork.content.contraptions.resistor;

import com.simibubi.create.content.contraptions.RotationPropagator;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.relays.encased.AbstractEncasedShaftBlock;
import com.simibubi.create.content.contraptions.relays.encased.SplitShaftTileEntity;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.ticks.TickPriority;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

import java.util.Random;

public class RedstoneResistorBlock extends AbstractEncasedShaftBlock implements ITE<SplitShaftTileEntity> {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public RedstoneResistorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(POWERED,
                context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        if (level.isClientSide)
            return;

        int redstone = level.getBestNeighborSignal(pos);

        boolean previouslyPowered = state.getValue(POWERED);
        if (previouslyPowered != (redstone > 0)) {
            level.setBlock(pos, state.cycle(POWERED), 2);
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof RedstoneResistorBlockEntity resistor) {
            resistor.onRedstoneUpdate(redstone);
        }
    }

    @Override
    public Class<SplitShaftTileEntity> getTileEntityClass() {
        return SplitShaftTileEntity.class;
    }

    @Override
    public BlockEntityType<? extends SplitShaftTileEntity> getTileEntityType() {
        return ClockWorkBlockEntities.REDSTONE_RESISTOR.get();
    }

    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, Random random) {
        BlockEntity te = worldIn.getBlockEntity(pos);
        if (te == null || !(te instanceof KineticTileEntity))
            return;
        KineticTileEntity kte = (KineticTileEntity) te;
        RotationPropagator.handleAdded(worldIn, pos, kte);
    }
}