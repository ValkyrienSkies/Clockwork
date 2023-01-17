package org.valkyrienskies.clockwork.content.contraptions.propellor;


import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.MechanicalBearingTileEntity;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.INamedIconOptions;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
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
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.IPropStreamSource;
import org.valkyrienskies.clockwork.content.contraptions.propellor.stream.PropStream;
import org.valkyrienskies.clockwork.content.forces.PropellorController;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorCreatePhysData;
import org.valkyrienskies.clockwork.fabric.content.contraptions.components.propellor.PropellorUpdatePhysData;
import org.valkyrienskies.clockwork.platform.api.Propellor;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PropellorBearingBlockEntity extends MechanicalBearingTileEntity implements Propellor, IPropStreamSource {
    protected ScrollOptionBehaviour<RotationDirection> movementDirection;
    protected boolean queuedReassembly;
    protected double lastGeneratedSpeed;

    public PropStream propStream;
    protected int airCurrentUpdateCooldown;
    protected int entitySearchCooldown;
    protected boolean updateAirFlow;

    float rotspeed=0;
    float targetSpeed=0;
    float lastSpeed=0;
    int sails;
    int moddingSpeed;
    public List<BlockPos> sailPositions;
    boolean slowingDown=false;
    float disassembling;
    int countDown = 200;
    float spinup;
    boolean spinningUp=false;

    private Integer physPropId = null;

    public PropellorBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        sailPositions = new ArrayList<>();
        propStream = new PropStream(this);
        updateAirFlow = true;
    }

//    @Override
//    public float getRotationSpeedModifier(Direction face) {
//        if (hasSource()) {
//            if (face != getSourceFacing() && getBlockState().getValue(BlockStateProperties.POWERED))
//                return 0;
//        }
//        return 1;
//    }


    public Direction getSourceFacing() {
        BlockPos localSource = source.subtract(getBlockPos());
        return Direction.getNearest(localSource.getX(), localSource.getY(), localSource.getZ());
    }


    @Override
    public void onSpeedChanged(float prevSpeed) {
        boolean cancelAssembly = assembleNextTick;
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = cancelAssembly;
        updateAirFlow = true;
    }
    private void modSpeed() {
        if (movedContraption == null) {
            return;
        }
        if (rotspeed == targetSpeed) {
            return;
        }
        float diff = targetSpeed - rotspeed;
        rotspeed = rotspeed + Mth.clamp(diff / 10, -32, 32);
//        float delta = Mth.clamp(, lastSpeed, 10)
        //rotspeed = (float) Mth.lerp(delta, lastSpeed, targetSpeed);
        speed = rotspeed;
        updateAirFlow = true;
    }
    private void modSlowdownSpeed() {
        disassembling--;
        if (((int) speed) == 0 && disassembling <= 0 || disassembling == 0) {
            if(!level.isClientSide) {
                disassemble();
            }
            slowingDown = false;
            return;
        }

        float stoppingPoint = (angle + rotspeed*disassembling*0.5f);
        float optimalStoppingPoint = 90f*Math.round(stoppingPoint/90f);
        float Q = (optimalStoppingPoint - stoppingPoint)/disassembling;
        rotspeed = (rotspeed + 6f*Q/disassembling)*(1f - 1f/disassembling);
        speed = rotspeed;
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
        if(level.isClientSide) {
            return;
        }
        spinup--;
        if (speed >= targetSpeed) {
            spinningUp = false;
            if (speed > targetSpeed) {
                speed = targetSpeed;
            }
            return;
        }

//            float time = 1f - (spinup / 20f);
//            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
        float startingPoint = (angle + targetSpeed*spinup*0.5f);
        float Q = (startingPoint)/spinup;
        rotspeed = (rotspeed + 6f*Q/spinup)*(1f - 1f/spinup);
        speed = rotspeed;
        updateAirFlow = true;
    }
    @Override
    public void tick() {
        super.tick();
        targetSpeed = getSourceSpeed();
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

        if (slowingDown) {
            modSlowdownSpeed();
        } else if (spinningUp) {
            modSpinupSpeed();
        }
        if (!spinningUp && !slowingDown) {
            modSpeed();
        }
        if (overStressed) {
            stressShutdown();
        }

        if (entitySearchCooldown-- <= 0) {
            entitySearchCooldown = 5;
            propStream.findEntities();
            propStream.findShips();
        }
        if (level.isClientSide()) {
//            particles();
            return;
        }
//        if (!queuedReassembly)
//            return;
        if (speed!=0) {
            lastGeneratedSpeed = speed;
        }
        if (lastGeneratedSpeed<0 && movementDirection.getValue()==0 || (lastGeneratedSpeed>0 && movementDirection.getValue()!=0)) {
            setBlockDirection(PropellorBearingBlock.Direction.PULL);
        } else {
            setBlockDirection(PropellorBearingBlock.Direction.PUSH);
        }
        propStream.tick();
//        queuedReassembly = false;
//        if (!running)
//            assembleNextTick = true;
//        if (assembleNextTickCW)
//            assembleNextTick = true;
//        if (!assembleNextTickCW)
//            assembleNextTick = false;


        if (physPropId != null) {
            if (server) {
                final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
                if (ship != null) {
                    final PropellorUpdatePhysData data = new PropellorUpdatePhysData(speed, angle);
                    PropellorController.getOrCreate(ship).updatePropellor(physPropId, data);
                }
            }
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
                return;
            }
        }
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
    public void disassembleForMovement() {
        if (!running)
            return;
        disassemble();
        queuedReassembly = true;
    }
    @Override
    public float calculateStressApplied() {
        if (!running)
            return 0;
        if (movedContraption != null) {
            sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks();
        }
        sails = Math.max(sails,2);
        return sails*2.0f;
    }
    @Override
    public boolean isPropellor() {
        return true;
    }
    @Override
    public float getAngularSpeed() {
        float speed = rotspeed;
        if (level.isClientSide) {
            speed*= ServerSpeedProvider.get();
            speed+=clientAngleDiff / 3f;
        }
        return speed;
    }
    @Override
    public void assemble() {
        rotspeed = 0;
        super.assemble();
        getSails();
        List<Vector3ic> sailVecs = sailPositions.stream().map(v -> (Vector3ic) VectorConversionsMCKt.toJOML(v)).toList();
        Vector3dc axis = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(PropellorBearingBlock.FACING).getNormal());
        Vector3dc vecpos = VectorConversionsMCKt.toJOMLD(getBlockPos());
        PropellorCreatePhysData data = new PropellorCreatePhysData(vecpos, axis, angle, speed, sailVecs);

        if (!level.isClientSide) {
            final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
            if (ship != null) {
                physPropId = PropellorController.getOrCreate(ship).addPropellor(data);
            }
        }
        // Vector3dc bearingPos, Vector3dc bearingAxis, double bearingAngle, double bearingSpeed, List<Vector3ic> propellorPositions

//        startSpinup();
//        sails = ((BearingContraption) movedContraption.getContraption()).getSailBlocks();
    }
    @Override
    public void disassemble() {
        rotspeed = 0;
        super.disassemble();
        this.speed = targetSpeed;
        clearKineticInformation();
        attachKinetics();
        if (physPropId != null) {
            if (!level.isClientSide) {
                final LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
                if (ship != null) {
                    PropellorController.getOrCreate(ship).removePropellor(physPropId);
                }
            }
        }
    }
    public void setAssembleNextTick(boolean bool) {
        assembleNextTick = bool;
    }
    public void startSlowdown() {
        if(!slowingDown) {
            slowingDown = true;
            disassembling = (int)Math.abs(targetSpeed);
        }
    }
    public void startSpinup() {
        if(!spinningUp) {
            spinningUp = true;
            assemble();
            spinup = (int)Math.abs(targetSpeed);
        }
    }
    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putDouble("LastGenerated", lastGeneratedSpeed);
        compound.putFloat("RotationSpeed", rotspeed);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        if (!wasMoved) {
            lastGeneratedSpeed = compound.getDouble("LastGenerated");
        }
        rotspeed = compound.getFloat("RotationSpeed");
        super.read(compound, clientPacket);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode.setValue(2);
        behaviours.remove(movementMode);
        movementDirection = new ScrollOptionBehaviour<>(RotationDirection.class,
                Lang.translateDirect("contraptions.propellor.rotation_direction"), this, getMovementModeSlot());
        movementDirection.requiresWrench();
        movementDirection.withCallback($ -> onDirectionChanged());
        behaviours.add(movementDirection);
    }

    private void onDirectionChanged() {
    BlockState state = getBlockState();
    PropellorBearingBlock.Direction previouslyPowered = state.getValue(PropellorBearingBlock.DIRECTION);
    if (previouslyPowered == PropellorBearingBlock.Direction.PULL)
        level.setBlock(getBlockPos(), state.cycle(PropellorBearingBlock.DIRECTION), 2);
    if (!running)
        return;
    if (!level.isClientSide)
        updateGeneratedRotation();
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
        if(movedContraption!=null) {
            Map<BlockPos, StructureTemplate.StructureBlockInfo> Blocks = ((BearingContraption) movedContraption.getContraption()).getBlocks();
            for (Map.Entry<BlockPos, StructureTemplate.StructureBlockInfo> entry : Blocks.entrySet()) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(entry.getValue().state)) {
                    sailPositions.add(entry.getKey());
                }
            }
        }
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

    //    public void particles() {
//        if (Math.abs(rotspeed)>0.01 && movedContraption != null && isRunning()) {
//            Level level = getLevel();
//            Direction direction = getBlockState().getValue(BlockStateProperties.FACING);
//            Vector3f speed = new Vector3f(direction.getNormal().getX(), direction.getNormal().getY(), direction.getNormal().getZ());
//
//            float dirScale = 1f;
//
//            if((rotspeed<0 && movementDirection.getValue()==0) || (rotspeed>0 && movementDirection.getValue()!=0)) {
//                dirScale *= -1;
//            }
//
//            float offset = 1.0f+dirScale*0.5f;
//            float speedScale = 0.2f*dirScale*Math.abs(rotspeed)/5f;
//            float particleCount = 0.1f*sailPositions.size()*Math.abs(rotspeed)/5f;
//
//            particleCount += Create.RANDOM.nextFloat()-1.0f;
//            for (int i=0;i<particleCount;i++) {
//                BlockPos sailPos = sailPositions.get(Create.RANDOM.nextInt(sailPositions.size()));
//                Vec3 floatPos = new Vec3(sailPos.getX(), sailPos.getY(), sailPos.getZ());
//                floatPos = movedContraption.applyRotation(floatPos,0);
//
//                Vec3 pos = VecHelper.getCenterOf(this.worldPosition).add(Vec3.atLowerCornerOf(direction.getNormal()).scale(offset).add(floatPos));
//
//                level.addParticle(new PropellorStreamParticleData(this.worldPosition),pos.x,pos.y,pos.z,speed.x()*speedScale,speed.y()*speedScale,speed.z()*speedScale);
//            }
//        }
//    }
    public static enum RotationDirection implements INamedIconOptions {

        CLOCKWISE(AllIcons.I_REFRESH), COUNTER_CLOCKWISE(AllIcons.I_ROTATE_CCW),

        ;

        private String translationKey;
        private AllIcons icon;

        private RotationDirection(AllIcons icon) {
            this.icon = icon;
            translationKey = "generic." + Lang.asId(name());
        }

        @Override
        public AllIcons getIcon() {
            return icon;
        }

        @Override
        public String getTranslationKey() {
            return translationKey;
        }

    }

}