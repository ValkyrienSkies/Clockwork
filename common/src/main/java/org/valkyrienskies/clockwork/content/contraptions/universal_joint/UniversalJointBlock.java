package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class UniversalJointBlock extends DirectionalKineticBlock implements ITE<UniversalJointBlockEntity> {
    public UniversalJointBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        Direction facing = state.getValue(FACING);
        return facing.getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
//        Direction facing = state.getValue(FACING);
//        if (face.equals(facing)) {
//            return true;
//        } else if (face.equals(facing.getOpposite())) {
//            return true;
//        }
//        return false;
        return true;
    }

    @Override
    public Class<UniversalJointBlockEntity> getTileEntityClass() {
        return UniversalJointBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends UniversalJointBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.UNIVERSAL_JOINT.get();
    }
}
