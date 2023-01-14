package org.valkyrienskies.clockwork.fabric.content.physicalities.motion.wing;

import com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.fabric.AllClockworkShapes;
import org.valkyrienskies.clockwork.fabric.util.blocktype.TriAxisBlockWithConnections;

public class WingBlock extends TriAxisBlockWithConnections {

    public WingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AllClockworkShapes.WING.get(pState.getValue(AXIS));
    }

//    @Override
//    public void onPlace (BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
//        super.onPlace(state, level, pos, oldState, isMoving);
//        int flag = 2;
//        Axis ax = oldState.getValue(AXIS);
//        connectivityUpdate(oldState, level, pos, flag, ax);
//    }
//
//    @Override
//    public void onRemove (BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
//        int flag = 2;
//        Axis ax = state.getValue(AXIS);
//        connectivityUpdate(newState, level, pos, flag, ax);
//        super.onRemove(state, level, pos, newState, isMoving);
//
//    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        int flag = 2 | 16;
        Axis ax = state.getValue(AXIS);
        connectivityUpdate(state, level, pos, block, fromPos, flag, ax);
    }

    private void connectivityUpdate(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, int flag, Axis ax) {
        BlockState neighbor1;
        BlockState neighbor2;
        BlockState neighbor3;
        BlockState neighbor4;

        BlockPos neighbor1Pos;
        BlockPos neighbor2Pos;
        BlockPos neighbor3Pos;
        BlockPos neighbor4Pos;

        if (ax == Axis.Y) {
            neighbor1 = level.getBlockState(pos.north(1));
            neighbor1Pos = pos.north(1);
            neighbor2 = level.getBlockState(pos.east(1));
            neighbor2Pos = pos.east(1);
            neighbor3 = level.getBlockState(pos.south(1));
            neighbor3Pos = pos.south(1);
            neighbor4 = level.getBlockState(pos.west(1));
            neighbor4Pos = pos.west(1);
        } else if (ax == Axis.X) {
            neighbor1 = level.getBlockState(pos.above(1));
            neighbor1Pos = pos.above(1);
            neighbor2 = level.getBlockState(pos.south(1));
            neighbor2Pos = pos.south(1);
            neighbor3 = level.getBlockState(pos.below(1));
            neighbor3Pos = pos.below(1);
            neighbor4 = level.getBlockState(pos.north(1));
            neighbor4Pos = pos.north(1);
        } else {
            neighbor1 = level.getBlockState(pos.above(1));
            neighbor1Pos = pos.above(1);
            neighbor2 = level.getBlockState(pos.west(1));
            neighbor2Pos = pos.west(1);
            neighbor3 = level.getBlockState(pos.below(1));
            neighbor3Pos = pos.below(1);
            neighbor4 = level.getBlockState(pos.east(1));
            neighbor4Pos = pos.east(1);
        }

        if (neighbor1.getBlock() instanceof WingBlock && neighbor1.getValue(AXIS) == ax) {
                level.setBlock(pos, state.setValue(connectedOne, true), flag);
                level.setBlock(neighbor1Pos, neighbor1.setValue(connectedThree, true), flag);
        } else if (neighbor1.getBlock() instanceof WingBlock && !(neighbor1.getValue(AXIS) == ax)) {
                level.setBlock(pos, state.setValue(connectedOne, false), flag);
        }
        if (neighbor2.getBlock() instanceof WingBlock && neighbor2.getValue(AXIS) == ax) {
                level.setBlock(pos, state.setValue(connectedTwo, true), flag);
                level.setBlock(neighbor2Pos, neighbor2.setValue(connectedFour, true), flag);
        } else if (neighbor2.getBlock() instanceof WingBlock && !(neighbor2.getValue(AXIS) == ax)) {
                level.setBlock(pos, state.setValue(connectedTwo, false), flag);
        }
        if (neighbor3.getBlock() instanceof WingBlock && neighbor3.getValue(AXIS) == ax) {
                level.setBlock(pos, state.setValue(connectedThree, true), flag);
                level.setBlock(neighbor3Pos, neighbor3.setValue(connectedOne, true), flag);
        } else if (neighbor3.getBlock() instanceof WingBlock && !(neighbor3.getValue(AXIS) == ax)) {
                level.setBlock(pos, state.setValue(connectedThree, false), flag);
        }
        if (neighbor4.getBlock() instanceof WingBlock && neighbor4.getValue(AXIS) == ax) {
                level.setBlock(pos, state.setValue(connectedFour, true), flag);
                level.setBlock(neighbor4Pos, neighbor4.setValue(connectedTwo, true), flag);
        } else if (neighbor4.getBlock() instanceof WingBlock && !(neighbor4.getValue(AXIS) == ax)) {
                level.setBlock(pos, state.setValue(connectedFour, false), flag);
        }
    }
}
