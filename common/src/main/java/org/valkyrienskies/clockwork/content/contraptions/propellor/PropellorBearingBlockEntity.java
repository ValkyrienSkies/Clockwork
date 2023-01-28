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
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.IPropStreamSource;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.PropStream;
import org.valkyrienskies.clockwork.content.forces.PropellorController;
import org.valkyrienskies.clockwork.platform.api.Propellor;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropellorBearingBlockEntity extends KineticTileEntity implements Propellor, IPropStreamSource, IBearingTileEntity {

    public PropStream propStream;
    public List<BlockPos> sailPositions;
    protected int airCurrentUpdateCooldown;
    protected int entitySearchCooldown;
    protected boolean updateAirFlow;
    boolean speedChanged = false;
    float rotspeed;
    float oldRotspeed;
    boolean assembleNextTick = false;
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
        return Mth.lerp(partialTicks, angle, angle + (rotspeed*3/10f));
    }
    @Override
    public float getRotspeed() {
        return rotspeed;
    }
    @Override
    public void tick() {
        super.tick();
        boolean server = !level.isClientSide || isVirtual();
        if (server && airCurrentUpdateCooldown == 0) {
            airCurrentUpdateCooldown = AllConfigs.SERVER.kinetics.fanBlockCheckRate.get();
            updateAirFlow = true;
        }
        if (updateAirFlow) {
            updateAirFlow = false;
            propStream.rebuild();
            sendData();
        }

        if (entitySearchCooldown-- <= 0) {
            entitySearchCooldown = 5;
            propStream.findEntities();
            propStream.findShips();
        }
        oldRotspeed = rotspeed;
        if (spinningUp) {
            modSpinupSpeed();
        } else if (slowingDown) {
            modSlowdownSpeed();
        } else {
            modSpeed();
        }

        if (overStressed) {
            stressShutdown();
        }

        if (rotspeed < 0) {
            setBlockDirection(PropellorBearingBlock.Direction.PULL);
        } else {
            setBlockDirection(PropellorBearingBlock.Direction.PUSH);
        }
        if (speedChanged) {
            onRotspeedChanged();
            speedChanged = false;
        }

        propStream.tick();
        prevAngle = angle;
        if (level.isClientSide)
            clientAngleDiff /= 2;

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (!running) {
                assemble();
            }
        }

        if (!running)
            return;

        if (!(movedContraption != null && movedContraption.isStalled())) {
            float angularSpeed = getAngularSpeed(rotspeed);
            float newAngle = angle + (rotspeed*3/10f);
            angle = (float) (newAngle % 360);
        }

        applyRotation();

        if (physPropId != null) {
            if (server) {
                final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
                if (ship != null) {
                    final PropellorUpdatePhysData data = new PropellorUpdatePhysData(rotspeed, angle);
                    PropellorController.getOrCreate(ship).updatePropellor(physPropId, data);
                }
            }
        }
    }

    private void modSpeed() {
        if (movedContraption == null) {
            return;
        }
        if (rotspeed == speed) {
            return;
        }
//        if ((int) getSourceSpeed() == 0 && (int) speed == 0) {
//            speed = 0;
//            return;
//        }
        float diff = speed - rotspeed;
        rotspeed = rotspeed + Mth.clamp(diff / 10, -32, 32);
//        float delta = Mth.clamp(, lastSpeed, 10)
        //rotspeed = (float) Mth.lerp(delta, lastSpeed, targetSpeed);
        updateAirFlow = true;
    }

    private void modSlowdownSpeed() {
        disassembling--;
        if (disassembling <= 0) {
            if (!level.isClientSide) {
                disassemble();
            }
            slowingDown = false;
            return;
        }

        float stoppingPoint = (angle + rotspeed * disassembling * 0.5f);
        float optimalStoppingPoint = 90f * Math.round(stoppingPoint / 90f);
        float Q = (optimalStoppingPoint - stoppingPoint) / disassembling;
        rotspeed = (rotspeed + 6f * Q / disassembling) * (1f - 1f / disassembling);
        updateAirFlow = true;
    }

    public float getSourceSpeed() {
        if (source == null || level == null) {
            return 0;
        }
        BlockEntity tileEnt = level.getBlockEntity(source);
        KineticTileEntity sourceTe = tileEnt instanceof KineticTileEntity ? (KineticTileEntity) tileEnt : null;
        if (sourceTe == null || sourceTe.getSpeed() == 0) {
            return 0;
        } else {
            return sourceTe.getSpeed();
        }
    }

    private void modSpinupSpeed() {
        if (level.isClientSide) {
            return;
        }
        spinup--;
        if (Math.abs(rotspeed) >= Math.abs(speed)) {
            spinningUp = false;
            if (Math.abs(rotspeed) > Math.abs(speed)) {
                rotspeed = speed;
            }
            return;
        }

//            float time = 1f - (spinup / 20f);
//            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
        float startingPoint = (angle + speed * spinup * 0.5f);
        float Q = (startingPoint) / spinup;
        rotspeed = (rotspeed + 6f * Q / spinup) * (1f - 1f / spinup);
        updateAirFlow = true;
    }

    @Override
    public float calculateStressApplied() {
        if (!running)
            return 0;
        if (movedContraption != null) {
            sails = ((PropellorContraption) movedContraption.getContraption()).getSailBlocks();
        }
        sails = Math.max(sails, 2);
        if (speed != 0) {
            return (sails * 2.0f * rotspeed)/speed;
        }
        return 0;
    }

    public float getAngularSpeed(float angSpeed) {
        if (level.isClientSide) {
            angSpeed *= ServerSpeedProvider.get();
            angSpeed += clientAngleDiff / 3f;
        }
        return angSpeed;
    }

//    public float getCurrentSpeed() {
//        float transitionSpeed = rotspeed;
//        if (spinningUp) {
//            transitionSpeed = getSpinupSpeed(transitionSpeed);
//            return transitionSpeed;
//        }
//        if (slowingDown) {
//            transitionSpeed = getSlowdownSpeed(transitionSpeed);
//            return transitionSpeed;
//        }
//        if (transitionSpeed != speed) {
//            float diff = speed - transitionSpeed;
//            transitionSpeed = transitionSpeed + Mth.clamp(diff / 10f, -32f, 32f);
////        float delta = Mth.clamp(, lastSpeed, 10)
//            //rotspeed = (float) Mth.lerp(delta, lastSpeed, targetSpeed);
//            if ((int) diff == 0) {
//                transitionSpeed = speed;
//            }
//            return transitionSpeed;
//        }
//
//        return transitionSpeed;
//    }

//    public float getSpinupSpeed(float spinupSpeed) {
//        if (level.isClientSide) {
//            return spinupSpeed;
//        }
//        spinup--;
//        if (spinupSpeed >= speed) {
//            spinningUp = false;
//            if (spinupSpeed > speed) {
//                spinupSpeed = speed;
//            }
//            return spinupSpeed;
//        }
//
////            float time = 1f - (spinup / 20f);
////            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
//        float startingPoint = (angle + spinupSpeed * spinup * 0.5f);
//        float Q = (startingPoint) / spinup;
//        spinupSpeed = (spinupSpeed + 6f * Q / spinup) * (1f - 1f / spinup);
//        return spinupSpeed;
//    }
//
//    public float getSlowdownSpeed(float slowdownSpeed) {
//        disassembling--;
//        if (((int) slowdownSpeed) == 0 && disassembling <= 0 || disassembling == 0) {
//            if (!level.isClientSide) {
//                disassemble();
//            }
//            slowingDown = false;
//            return slowdownSpeed;
//        }
//
//        float stoppingPoint = (angle + slowdownSpeed * disassembling * 0.5f);
//        float optimalStoppingPoint = 90f * Math.round(stoppingPoint / 90f);
//        float Q = (optimalStoppingPoint - stoppingPoint) / disassembling;
//        slowdownSpeed = (slowdownSpeed + 6f * Q / disassembling) * (1f - 1f / disassembling);
//        return slowdownSpeed;
//    }

//    protected void applyRotation() {
//        BlockState blockState = getBlockState();
//        Direction.Axis axis = Direction.Axis.X;
//
//        if (blockState.hasProperty(PropellorBearingBlock.FACING))
//            axis = blockState.getValue(PropellorBearingBlock.FACING)
//                    .getAxis();
//
//        if (movedContraption != null) {
//            movedContraption.setAngle(angle);
//            movedContraption.setRotationAxis(axis);
//        }
//    }

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
        compound.putFloat("Rotspeed", rotspeed);
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        AssemblyException.write(compound, lastException);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        float angleBefore = angle;
        rotspeed = compound.getFloat("Rotspeed");
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
            movedContraption = null;
        }
    }

//    @Override
//    public void attach(ControlledContraptionEntity contraption) {
//        BlockState blockState = getBlockState();
//        if (!(contraption.getContraption() instanceof PropellorContraption))
//            return;
//        if (!blockState.hasProperty(BearingBlock.FACING))
//            return;
//
//        this.movedContraption = contraption;
//        setChanged();
//        BlockPos anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING));
//        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
//        if (!level.isClientSide) {
//            this.running = true;
//            sendData();
//        }
//    }


    @Override
    public void attach(ControlledContraptionEntity contraption) {
        if (!(contraption.getContraption() instanceof PropellorContraption cc))
            return;

        setChanged();
        Direction facing = getBlockState().getValue(PropellorBearingBlock.FACING);
        BlockPos anchor = worldPosition.relative(facing, cc.offset + 1);

        this.movedContraption = contraption;
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

    public void setAngle(float forcedAngle) {
        angle = forcedAngle;
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
        return this.getBlockState().getValue(BlockStateProperties.FACING);
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
                .getBlock() instanceof PropellorBearingBlock))
            return;

        Direction direction = getBlockState().getValue(PropellorBearingBlock.FACING);
        PropellorContraption contraption;

        try {
            contraption = PropellorContraption.assembleProp(level, worldPosition, direction);
            lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            sendData();
            return;
        }
        speedChanged = rotSpeedChanged();
        if (contraption == null)
            return;
        if (contraption.getBlocks()
                .isEmpty())
            return;
        BlockPos anchor = worldPosition.relative(direction);

        contraption.removeBlocksFromWorld(level, BlockPos.ZERO);
        movedContraption = ControlledContraptionEntity.create(level, this, contraption);
        movedContraption.setPos(anchor.getX(), anchor.getY(), anchor.getZ());
        movedContraption.setRotationAxis(direction.getAxis());
        level.addFreshEntity(movedContraption);

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        running = true;
        angle = 0;
        rotspeed = 0;
        spinningUp = true;
        spinup = (int) Math.abs(speed);
        getSails();
        List<Vector3ic> sailVecs = sailPositions.stream().map(v -> (Vector3ic) VectorConversionsMCKt.toJOML(v)).toList();
        Vector3dc axis = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(BlockStateProperties.FACING).getNormal());
        Vector3dc vecpos = VectorConversionsMCKt.toJOMLD(getBlockPos());
        PropellorCreatePhysData data = new PropellorCreatePhysData(vecpos, axis, angle, rotspeed, sailVecs);

        if (!level.isClientSide) {
            final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
            if (ship != null) {
                physPropId = PropellorController.getOrCreate(ship).addPropellor(data);
            }
        }
        sendData();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (movedContraption != null && !level.isClientSide)
            sendData();
    }

    public void shutDown() {
        slowingDown = true;
        disassembling = (int) Math.abs(rotspeed);
        spinningUp = false;
        spinup = 0;
    }
    public void disassemble() {
        if (!running && movedContraption == null)
            return;
        rotspeed = 0;
        angle = 0;
        slowingDown = false;
        disassembling = 0;
        if (movedContraption != null) {
            movedContraption.disassemble();
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
        }

        movedContraption = null;
        running = false;
        if (physPropId != null) {
            if (!level.isClientSide) {
                final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
                if (ship != null) {
                    PropellorController.getOrCreate(ship).removePropellor(physPropId);
                }
            }
        }
        sendData();
    }

    @Override
    public float getMaxDistance() {
        return IPropStreamSource.super.getMaxDistance();
    }

    @Override
    public boolean isPropellor() {
        return true;
    }
    public boolean rotSpeedChanged() {
        if (rotspeed != oldRotspeed) {
            return true;
        }
        return false;
    }

    @Override
    public void onRotspeedChanged() {
        updateAirFlow = true;
    }
    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        updateAirFlow = true;
    }

    @Override
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        if (!(contraption.getContraption() instanceof PropellorContraption cc))
            return false;

        return this.movedContraption == contraption;
    }


    @Override
    public void onStall() {
        if (!level.isClientSide)
            sendData();
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
