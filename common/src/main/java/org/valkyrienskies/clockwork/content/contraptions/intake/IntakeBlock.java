package org.valkyrienskies.clockwork.content.contraptions.intake;

import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.ClockWorkBlockEntities;

import java.util.Random;

public class IntakeBlock extends DirectionalKineticBlock implements ITE<IntakeBlockEntity> {

    public IntakeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public SpeedLevel getMinimumRequiredSpeedLevel() {
        return super.getMinimumRequiredSpeedLevel();
    }

    @Override
    public boolean hideStressImpact() {
        return super.hideStressImpact();
    }

    @Override
    public boolean showCapacityWithAnnotation() {
        return super.showCapacityWithAnnotation();
    }

    @Override
    public Class<IntakeBlockEntity> getTileEntityClass() {
        return IntakeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IntakeBlockEntity> getTileEntityType() {
        return ClockWorkBlockEntities.INTAKE.get();
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        Direction dir = stateIn.getValue(FACING);
        double x = pos.getX() + ((dir.getStepX() + 1) * 0.5);
        double y = pos.getY() + ((dir.getStepY() + 1) * 0.5);
        double z = pos.getZ() + ((dir.getStepZ() + 1) * 0.5);
        IntakeBlockEntity te = getTileEntity(worldIn, pos);
        double speed = 0;
        if (te != null) {
            speed = te.getSpeed() / 100;
        }
        double speedX = dir.getStepX() * speed;
        double speedY = dir.getStepY() * speed;
        double speedZ = dir.getStepZ() * speed;

        for (int i = 0; i < 16; i++) {
            double x2 = rand.nextDouble() * 0.2 - 1;
            double y2 = rand.nextDouble() * 0.2 - 1;
            double z2 = rand.nextDouble() * 0.2 - 1;
            worldIn.addParticle(ParticleTypes.SMOKE, x2, y2, z2, -speedX, -speedY, -speedZ);
        }
    }
}
