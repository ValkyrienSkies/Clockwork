package org.valkyrienskies.clockwork.content.contraptions.flap;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption;
import org.valkyrienskies.clockwork.platform.api.IFlap;

import java.util.List;

public class FlapBearingBlockEntity extends KineticTileEntity implements IFlap, IBearingTileEntity {

    public boolean redstoneSideOne;
    public boolean redstoneSideTwo;
    protected float angle;
    protected float clientAngleDiff;
    protected boolean running;
    protected boolean assembleNextTick;
    protected AssemblyException lastException;
    protected ControlledContraptionEntity flap;
    private float prevForcedAngle;

    public FlapBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public boolean isFlap() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (flap != null) {
            flap.tick();
        }

        if (level.isClientSide) {
            prevForcedAngle = angle;
            clientAngleDiff /= 2;
        }

        if (getBlockState().getValue(FlapBearingBlock.FACING) == Direction.UP || getBlockState().getValue(FlapBearingBlock.FACING) == Direction.DOWN) {
            redstoneSideOne = level.hasNeighborSignal(worldPosition.relative(Direction.EAST));
            redstoneSideTwo = level.hasNeighborSignal(worldPosition.relative(Direction.WEST));
        } else if (getBlockState().getValue(FlapBearingBlock.FACING) == Direction.NORTH || getBlockState().getValue(FlapBearingBlock.FACING) == Direction.SOUTH) {
            redstoneSideOne = level.hasNeighborSignal(worldPosition.relative(Direction.EAST));
            redstoneSideTwo = level.hasNeighborSignal(worldPosition.relative(Direction.WEST));
        } else if (getBlockState().getValue(FlapBearingBlock.FACING) == Direction.EAST || getBlockState().getValue(FlapBearingBlock.FACING) == Direction.WEST) {
            redstoneSideOne = level.hasNeighborSignal(worldPosition.relative(Direction.NORTH));
            redstoneSideTwo = level.hasNeighborSignal(worldPosition.relative(Direction.SOUTH));
        }

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (running) {
                boolean canDisassemble = true;
                if (speed == 0 && (canDisassemble || flap == null || flap.getContraption()
                        .getBlocks()
                        .isEmpty())) {
                    if (flap != null)
                        flap.getContraption()
                                .stop(level);
                    disassemble();
                }
                return;
            } else
                assemble();
            return;
        }

        if (!(flap != null && flap.isStalled())) {
            float testSpeed = getAngularSpeed() / 2f;
            float newAngle = angle + getFlapSpeed();
            angle = newAngle % 360;
        }

        if (!running)
            return;

        applyRotations();
    }


    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        AssemblyException.write(compound, lastException);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        float angleBefore = angle;

        running = compound.getBoolean("Running");
        angle = compound.getFloat("Angle");
        lastException = AssemblyException.read(compound);
        super.read(compound, clientPacket);

        if (!clientPacket)
            return;

        if (running) {
            clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle);
            angle = angleBefore;
        } else {
            flap = null;
        }
    }

    public void assemble() {
        if (!(level.getBlockState(worldPosition)
                .getBlock() instanceof FlapBearingBlock))
            return;

        Direction direction = getBlockState().getValue(FlapBearingBlock.FACING);

        FlapContraption contraption;

        try {
            contraption = FlapContraption.assembleFlap(level, worldPosition, direction);
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }
        if (contraption == null)
            return;
        if (contraption.getBlocks()
                .isEmpty())
            return;
        BlockPos anchor = worldPosition.relative(direction);

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        flap = ControlledContraptionEntity.create(level, this, contraption);
        flap.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        flap.setRotationAxis(direction.getAxis());
        level.addFreshEntity(flap);

        //Run
        running = true;
        angle = 0;
        sendData();
    }

    public void disassemble() {
        if (!running && flap == null)
            return;

        angle = 0;
        applyRotations();
        if (flap != null) {
            flap.disassemble();
        }

        flap = null;
        running = false;
        sendData();
    }

    protected void applyRotations() {
        BlockState blockState = getBlockState();
        Direction.Axis axis = Direction.Axis.X;

        if (blockState.hasProperty(FlapBearingBlock.FACING))
            axis = blockState.getValue(FlapBearingBlock.FACING)
                    .getAxis();

        if (flap != null) {
            flap.setAngle(angle);
            flap.setRotationAxis(axis);
        }
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        if (!(contraption.getContraption() instanceof FlapContraption cc))
            return;

        setChanged();
        Direction facing = getBlockState().getValue(FlapBearingBlock.FACING);
        BlockPos anchor = worldPosition.relative(facing, cc.offset + 1);

        this.flap = contraption;
        flap.setPos(anchor.getX(), anchor.getY(), anchor.getZ());

        if (!level.isClientSide) {
            this.running = true;
            sendData();
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (flap != null && !level.isClientSide)
            sendData();
    }

    public float getFlapSpeed() {
        float speed = getAngularSpeed() / 2f;

        if (speed != 0) {

            float flapTarget = getFlapTarget(redstoneSideOne, redstoneSideTwo);
            float shortestAngleDiff = AngleHelper.getShortestAngleDiff(angle, flapTarget);
            if (shortestAngleDiff < 0) {
                speed = Math.max(speed, shortestAngleDiff);
            } else {
                speed = Math.min(-speed, shortestAngleDiff);
            }
        }

        return speed + clientAngleDiff / 3f;
    }

    protected float getFlapTarget(Boolean negativeActivated, Boolean positiveActivated) {
        if (negativeActivated && !positiveActivated) {
            return -22.5f;
        }
        if (positiveActivated && !negativeActivated) {
            return 22.5f;
        }
        if (negativeActivated && positiveActivated) {
            return 0;
        }
        return 0;
    }

    public boolean isRunning() {
        return running;
    }

    public void setAngle(float forcedAngle) {
        angle = forcedAngle;
    }

    public float getAngularSpeed() {
        float speed = -Math.abs(getSpeed() * 3 / 10f);
        if (level.isClientSide)
            speed *= ServerSpeedProvider.get();
        return speed;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks, prevForcedAngle, angle);
        if (flap == null || flap.isStalled())
            partialTicks = 0;
        return Mth.lerp(partialTicks, angle, angle + getFlapSpeed());
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        if (!(contraption.getContraption() instanceof FlapContraption cc))
            return false;

        return this.flap == contraption;
    }

    @Override
    public void onStall() {
        if (!level.isClientSide)
            sendData();
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = true;
    }

    @Override
    public boolean isValid() {
        return !isRemoved();
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }
}
