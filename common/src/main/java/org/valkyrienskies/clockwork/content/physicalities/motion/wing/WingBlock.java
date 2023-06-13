package org.valkyrienskies.clockwork.content.physicalities.motion.wing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.clockwork.ClockWorkShapes;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;
import org.valkyrienskies.core.api.ships.Wing;

public class WingBlock extends ConnectedWingAlike implements org.valkyrienskies.mod.common.block.WingBlock {

    public WingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return ClockWorkShapes.WING.get(switch (pState.getValue(FACING)) {
            case EAST, WEST -> Axis.X;
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
        });
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        level.setBlockAndUpdate(pos, getNewState(state, level, pos));
    }

    @Override
    public BlockState getNewState(BlockState state, Level level, BlockPos pos) {
        Direction facing = state.getValue(FACING);

        BlockState north = level.getBlockState(pos.north());
        BlockState south = level.getBlockState(pos.south());
        BlockState east = level.getBlockState(pos.east());
        BlockState west = level.getBlockState(pos.west());
        BlockState up = level.getBlockState(pos.above());
        BlockState down = level.getBlockState(pos.below());

        return switch (facing) {
            case NORTH, SOUTH -> state
                    .setValue(NORTH, false)
                    .setValue(SOUTH, false)
                    .setValue(EAST, (east.getBlock() instanceof WingBlock) && east.getValue(FACING).equals(facing))
                    .setValue(WEST, (west.getBlock() instanceof WingBlock) && west.getValue(FACING).equals(facing))
                    .setValue(UP, (up.getBlock() instanceof WingBlock) && up.getValue(FACING).equals(facing))
                    .setValue(DOWN, (down.getBlock() instanceof WingBlock) && down.getValue(FACING).equals(facing))
                    .setValue(FACING, Direction.NORTH);
            case EAST, WEST -> state
                    .setValue(NORTH, (north.getBlock() instanceof WingBlock) && north.getValue(FACING).equals(facing))
                    .setValue(SOUTH, (south.getBlock() instanceof WingBlock) && south.getValue(FACING).equals(facing))
                    .setValue(EAST, false)
                    .setValue(WEST, false)
                    .setValue(UP, (up.getBlock() instanceof WingBlock) && up.getValue(FACING).equals(facing))
                    .setValue(DOWN, (down.getBlock() instanceof WingBlock) && down.getValue(FACING).equals(facing))
                    .setValue(FACING, Direction.EAST);
            case UP, DOWN -> state
                    .setValue(NORTH, (north.getBlock() instanceof WingBlock) && north.getValue(FACING).equals(facing))
                    .setValue(SOUTH, (south.getBlock() instanceof WingBlock) && south.getValue(FACING).equals(facing))
                    .setValue(EAST, (east.getBlock() instanceof WingBlock) && east.getValue(FACING).equals(facing))
                    .setValue(WEST, (west.getBlock() instanceof WingBlock) && west.getValue(FACING).equals(facing))
                    .setValue(UP, false)
                    .setValue(DOWN, false)
                    .setValue(FACING, Direction.UP);
        };
    }

    @Override
    public Wing getWing(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        double wingPower = 150;
        double wingDrag = 150;
        double wingBreakingForce = 10;
        double wingCamberAttackingBias = Math.toRadians(10.0);
        return switch (blockState.getValue(FACING)) {
            case EAST, WEST -> new Wing(new Vector3d(1, 0, 0), wingPower, wingDrag, wingBreakingForce, wingCamberAttackingBias);
            case UP, DOWN -> new Wing(new Vector3d(0, 1, 0), wingPower, wingDrag, wingBreakingForce, wingCamberAttackingBias);
            case NORTH, SOUTH -> new Wing(new Vector3d(0, 0, 1), wingPower, wingDrag, wingBreakingForce, wingCamberAttackingBias);
        };
    }
}
