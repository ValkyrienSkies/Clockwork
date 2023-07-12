package org.valkyrienskies.clockwork.content.munitions.stationary.solver;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class SolverBlock extends DirectionalKineticBlock implements IBE<SolverBlockEntity>, BeaconBeamBlock, ICogWheel {
    public SolverBlock(Properties properties) {
        super(properties);
    }

    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return SpeedLevel.MEDIUM;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return false;
    }

    @Override
    public Class<SolverBlockEntity> getBlockEntityClass() {
        return SolverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolverBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.SOLVER.get();
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.PURPLE;
    }
}
