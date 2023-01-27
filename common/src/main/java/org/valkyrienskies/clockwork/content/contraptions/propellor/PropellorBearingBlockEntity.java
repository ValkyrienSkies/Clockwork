package org.valkyrienskies.clockwork.content.contraptions.propellor;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.IPropStreamSource;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.PropStream;
import org.valkyrienskies.clockwork.platform.api.Propellor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropellorBearingBlockEntity extends KineticTileEntity implements Propellor, IPropStreamSource, IBearingTileEntity {

    public PropStream propStream;
    public List<BlockPos> sailPositions;
    protected ScrollOptionBehaviour<PropellorBearingBlockEntityOLD.RotationDirection> movementDirection;
    protected int airCurrentUpdateCooldown;
    protected int entitySearchCooldown;
    protected boolean updateAirFlow;
    float rotspeed = 0;

    int sails;
    int moddingSpeed;
    boolean slowingDown = false;
    float disassembling;
    int countDown = 200;
    float spinup;
    boolean spinningUp = false;
    protected float angle;
    protected boolean running;
    protected float clientAngleDiff;
    protected AssemblyException lastException;
    protected ControlledContraptionEntity movedContraption;
    private float prevAngle;
    private float prevSpeed;

    private Integer physPropId = null;

    public PropellorBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sailPositions = new ArrayList<>();
        propStream = new PropStream(this);
        updateAirFlow = true;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, prevAngle, angle);
        if (movedContraption == null || movedContraption.isStalled() || !running)
            partialTicks = 0;
        return Mth.lerp(partialTicks, angle, angle + getCurrentSpeed());
    }

    public float getAngularSpeed() {
        float speed = getSpeed();
        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
    }

    public float getCurrentSpeed() {
        if (spinningUp) {
            return getSpinupSpeed(rotspeed);
        }

        return getAngularSpeed() * speedModifier;
    }

    public float getSpinupSpeed(float spinupSpeed) {
        if (level.isClientSide) {
            return spinupSpeed;
        }
        spinup--;
        if (spinupSpeed >= speed) {
            spinningUp = false;
            if (spinupSpeed > speed) {
                spinupSpeed = speed;
            }
            return spinupSpeed;
        }

//            float time = 1f - (spinup / 20f);
//            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
        float startingPoint = (angle + speed * spinup * 0.5f);
        float Q = (startingPoint) / spinup;
        spinupSpeed = (spinupSpeed + 6f * Q / spinup) * (1f - 1f / spinup);
        return spinupSpeed;
    }

    public float getSlowdownSpeed(float slowdownSpeed) {
        return slowdownSpeed;
    }

    protected void applyRotation() {
        if (movedContraption == null)
            return;
        movedContraption.setAngle(angle);
        BlockState blockState = getBlockState();
        if (blockState.hasProperty(BlockStateProperties.FACING))
            movedContraption.setRotationAxis(blockState.getValue(BlockStateProperties.FACING)
                    .getAxis());
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("RotationSpeed", rotspeed);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        rotspeed = compound.getFloat("RotationSpeed");
        super.read(compound, clientPacket);
    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
        BlockState blockState = getBlockState();
        if (!(contraption.getContraption() instanceof PropellorContraption))
            return;
        if (!blockState.hasProperty(BearingBlock.FACING))
            return;

        this.movedContraption = contraption;
        setChanged();
        BlockPos anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING));
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        if (!level.isClientSide) {
            this.running = true;
            sendData();
        }
    }

    private void onDirectionChanged() {
        BlockState state = getBlockState();
        PropellorBearingBlock.Direction previouslyPowered = state.getValue(PropellorBearingBlock.DIRECTION);
        if (previouslyPowered == PropellorBearingBlock.Direction.PULL)
            level.setBlock(getBlockPos(), state.cycle(PropellorBearingBlock.DIRECTION), 2);
        if (!running)
            return;
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    public PropellorBearingBlock.Direction getDirectonFromBlock() {
        return PropellorBearingBlock.getDirectionof(getBlockState());
    }

    protected void setBlockDirection(PropellorBearingBlock.Direction direction) {
        PropellorBearingBlock.Direction inBlockState = getDirectonFromBlock();
        if (inBlockState == direction)
            return;
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(PropellorBearingBlock.DIRECTION, direction));
        notifyUpdate();
    }

    void getSails() {
        sailPositions = new ArrayList<>();
        if (movedContraption != null) {
            Map<BlockPos, StructureTemplate.StructureBlockInfo> Blocks = movedContraption.getContraption().getBlocks();
            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : Blocks.entrySet()) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(entry.getValue().state)) {
                    sailPositions.add(entry.getKey());
                }
            }
        }
    }

    @Override
    public void setAngle(float forcedAngle) {

    }

    @Override
    public int getSailCount() {
        return sailPositions.size();
    }

    @Override
    public PropStream getStream() {
        return propStream;
    }

    @Override
    public Level getStreamWorld() {
        return level;
    }

    @Override
    public BlockPos getStreamPos() {
        return worldPosition.offset(this.getBlockState().getValue(PropellorBearingBlock.FACING).getNormal());
    }

    @Override
    public Direction getStreamOriginSide() {
        return this.getBlockState().getValue(PropellorBearingBlock.FACING);
    }

    @Override
    public Direction getStreamDirection() {
        float speed = getSpeed();
        if (speed == 0)
            return null;
        Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
        speed = convertToDirection(speed, facing);
        return speed > 0 ? facing : facing.getOpposite();
    }

    @Override
    public boolean isSourceRemoved() {
        return remove;
    }

    @Override
    public Vector3d getStreamScale() {
        Vector3d distance = new Vector3d(1, 1, 1);

        //facing Z
        if (this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.NORTH || this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.SOUTH) {

            sailPositions.forEach(pos -> {
                if (Math.abs(pos.getX()) > Math.abs(this.worldPosition.getX())) {
                    if (Math.abs(pos.getX()) > distance.x) {
                        distance.x = pos.getX();
                    }
                }
                if (Math.abs(pos.getY()) > Math.abs(this.worldPosition.getY())) {
                    if (Math.abs(pos.getY()) > distance.y) {
                        distance.y = pos.getY();
                    }
                }
            });

        } else if (this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.WEST || this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.EAST) {
            sailPositions.forEach(pos -> {
                if (Math.abs(pos.getZ()) > Math.abs(this.worldPosition.getZ())) {
                    if (Math.abs(pos.getZ()) > distance.z) {
                        distance.z = pos.getZ();
                    }
                }
                if (Math.abs(pos.getY()) > Math.abs(this.worldPosition.getY())) {
                    if (Math.abs(pos.getY()) > distance.y) {
                        distance.y = pos.getY();
                    }
                }
            });
        } else if (this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.UP || this.getBlockState().getValue(PropellorBearingBlock.FACING) == Direction.DOWN) {
            sailPositions.forEach(pos -> {
                if (Math.abs(pos.getZ()) > Math.abs(this.worldPosition.getZ())) {
                    if (Math.abs(pos.getZ()) > distance.x) {
                        distance.z = pos.getZ();
                    }
                }
                if (Math.abs(pos.getX()) > Math.abs(this.worldPosition.getX())) {
                    if (Math.abs(pos.getX()) > distance.x) {
                        distance.x = pos.getX();
                    }
                }
            });
        } else {
            return distance;
        }
        return distance;
    }

    private void refreshKineticState() {
        if (isSourceRemoved()) {
            detachKinetics();
        } else if (source == null) {
            detachKinetics();
        }
    }

    private void stressShutdown() {
        if (Math.abs(speed) < 3f) {
            countDown--;
            if (countDown <= 0) {
                if (!level.isClientSide) {
                    disassemble();
                    countDown = 200;
                }
            }
        }
    }

    @Override
    public void remove() {
        if (!level.isClientSide)
            disassemble();
        super.remove();
    }

    public void assemble() {
        if (!(level.getBlockState(worldPosition)
                .getBlock() instanceof BearingBlock))
            return;

        Direction direction = getBlockState().getValue(BearingBlock.FACING);
        PropellorContraption contraption = new PropellorContraption(direction);
        try {
            if (!contraption.assemble(level, worldPosition))
                return;

            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        BlockPos anchor = worldPosition.relative(direction);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        if (contraption.containsBlockBreakers())
            award(AllAdvancements.CONTRAPTION_ACTORS);

        running = true;
        angle = 0;
        spinningUp = true;
        spinup = speed;
        sendData();
    }

    public void disassemble() {
        if (!running && movedContraption == null)
            return;
        angle = 0;
        if (movedContraption != null) {
            movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        movedContraption = null;
        running = false;
        sendData();
    }

    @Override
    public float getMaxDistance() {
        return IPropStreamSource.super.getMaxDistance();
    }

    @Override
    public boolean isPropellor() {
        return false;
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        updateAirFlow = true;
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        return false;
    }


    @Override
    public void onStall() {

    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public BlockPos getBlockPosition() {
        return null;
    }
}
