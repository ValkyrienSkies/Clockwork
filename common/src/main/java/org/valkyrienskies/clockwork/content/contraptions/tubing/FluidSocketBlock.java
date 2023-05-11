package org.valkyrienskies.clockwork.content.contraptions.tubing;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.content.contraptions.relays.elementary.ICogWheel;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class FluidSocketBlock extends DirectionalKineticBlock implements ITE<FluidSocketBlockEntity>, ICogWheel, SimpleWaterloggedBlock {
    public FluidSocketBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis();
    }

    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState.setValue(FACING, originalState.getValue(FACING)
                .getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_,
                               CollisionContext p_220053_4_) {
        return AllShapes.PUMP.get(state.getValue(FACING));
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        return super.updateAfterWrenched(newState, context);
    }

    @Override
    public Class<FluidSocketBlockEntity> getTileEntityClass() {
        return FluidSocketBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FluidSocketBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.FLUID_SOCKET.get();
    }
}
