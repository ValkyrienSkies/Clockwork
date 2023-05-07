package org.valkyrienskies.clockwork.content.contraptions.solver;

import com.simibubi.create.content.contraptions.base.KineticBlock;
import com.simibubi.create.content.contraptions.relays.elementary.ICogWheel;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;
import org.valkyrienskies.clockwork.ClockWorkBlocks;

public class SolverBlock extends KineticBlock implements ITE<SolverBlockEntity>, BeaconBeamBlock, ICogWheel {
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
    public Class<SolverBlockEntity> getTileEntityClass() {
        return SolverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SolverBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.SOLVER.get();
    }

    @Override
    public DyeColor getColor() {
        return DyeColor.PURPLE;
    }
}
