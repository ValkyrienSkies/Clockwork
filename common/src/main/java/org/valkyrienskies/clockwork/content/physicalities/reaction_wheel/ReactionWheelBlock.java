package org.valkyrienskies.clockwork.content.physicalities.reaction_wheel;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

public class ReactionWheelBlock extends RotatedPillarKineticBlock implements IBE<ReactionWheelBlockEntity> {
    public ReactionWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return AllShapes.LARGE_GEAR.get(pState.getValue(AXIS));
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public Class<ReactionWheelBlockEntity> getBlockEntityClass() {
        return ReactionWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ReactionWheelBlockEntity> getBlockEntityType() {
        return ClockWorkBlockEntities.REACTIONWHEEL.get();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        getBlockEntity(level, pos).setShouldRemove();
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public float getParticleTargetRadius() {
        return 2f;
    }

    @Override
    public float getParticleInitialRadius() {
        return 1.75f;
    }
}
