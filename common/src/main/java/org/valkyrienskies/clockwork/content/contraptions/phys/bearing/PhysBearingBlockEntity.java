package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.valkyrienskies.clockwork.ClockWorkSounds;
import org.valkyrienskies.clockwork.content.forces.physContraption.PhysBearingController;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.clockwork.platform.api.GlueType;
import org.valkyrienskies.clockwork.util.assemble.GlueAssembler;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.constraints.*;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import org.valkyrienskies.clockwork.util.animation.EaseHelper;

import java.lang.Math;
import java.util.List;

import static org.valkyrienskies.clockwork.util.animation.EaseHelper.easeInOutSine;
import static org.valkyrienskies.clockwork.util.animation.EaseHelper.easeOutBounce;

public class PhysBearingBlockEntity extends GeneratingKineticTileEntity implements IBearingTileEntity, IDisplayAssemblyExceptions, ContraptionController {
    protected ScrollOptionBehaviour<LockedMode> movementMode;
    boolean shouldRefresh = false;
    protected float angle;
    protected boolean running;
    protected boolean assembleNextTick;
    protected float clientAngleDiff;
    protected AssemblyException lastException;
    protected boolean disassembleWhenPossible = false;

    private float prevAngle;

    private long shiptraptionID = NO_SHIPTRAPTION_ID;

    private Integer bearingID = null;

    float coreAngle = 0;
    float previousCoreAngle = 0;

    boolean opening = false;

    boolean open = false;

    boolean closing = false;

    private float openProgress = 0;

    private float closeProgress = 0;

    private float inOutCorner = 0;
    private boolean cornerShrinking = false;

    public static final long NO_SHIPTRAPTION_ID = -1;

    public PhysBearingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(3);
    }

    @Override
    public boolean isWoodenTop() {
        return false;
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        movementMode = new ScrollOptionBehaviour<>(LockedMode.class, new TextComponent("Locked or Unlocked"),
                this, getMovementModeSlot());
        movementMode.requiresWrench();
        behaviours.add(movementMode);
    }

    @Override
    public void remove() {
//        if (!level.isClientSide)
//            disassemble();
        super.remove();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        if (bearingID != null) {
            compound.putInt("BearingID", bearingID);
        }
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            compound.putLong("ShiptraptionID", shiptraptionID);
        }
        AssemblyException.write(compound, lastException);
        compound.putBoolean("Open", open);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        if (wasMoved) {
            super.read(compound, clientPacket);
            return;
        }

        float angleBefore = angle;
        open = compound.getBoolean("Open");
        running = compound.getBoolean("Running");
        angle = compound.getFloat("Angle");
        lastException = AssemblyException.read(compound);
        if (compound.contains("BearingID")) {
            bearingID = compound.getInt("BearingID");
        }

        if (compound.contains("ShiptraptionID")) {
            shiptraptionID = compound.getLong("ShiptraptionID");
        }
        if (running) {
            if (shiptraptionID == NO_SHIPTRAPTION_ID) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle);
                angle = angleBefore;
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID;
        }

        shouldRefresh = true;

        super.read(compound, clientPacket);

        if (!clientPacket)
            return;


    }

    public float getCoreOffset(float partialTicks) {
        if (!running) {
            return 0;
        }

        return (float) Math.sin(easeInOutSine(inOutCorner))/5f;
    }

    public float getFlapRotOffset(float partialTicks) {
        if (!running) {
            return 0;
        } else if (opening) {
            return Mth.lerp(easeOutBounce(openProgress), 0, 70f);
        }
        return 70f + (float) Math.sin(inOutCorner);
    }

    public float getCornerHorizontalOffset(float partialTicks) {
        if (!running) {
            return 0;
        } else if (opening) {
            return Mth.lerp(easeInOutSine(openProgress), 0, 3f/16f + (float) Math.sin(easeInOutSine(inOutCorner))/16f);
        }
        return 3f/16f + (float) Math.sin(easeInOutSine(inOutCorner))/16f;
    }

    public float getCornerVerticalOffset(float partialTicks) {
        if (!running) {
            return 0;
        } else if (opening) {
            return Mth.lerp(easeInOutSine(openProgress), 0, 1f/16f + (float) Math.sin(easeInOutSine(inOutCorner))/16f);
        }
        return 1f/16f + (float) Math.sin(easeInOutSine(inOutCorner))/16f;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, prevAngle, angle);
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !running)
            partialTicks = 0;
        return Mth.lerp(partialTicks, angle, angle + getAngularSpeed());
    }

    public float getInterpolatedCoreAngle(float partialTicks) {

        previousCoreAngle = coreAngle;

        coreAngle++;

        if (coreAngle == 360) {
            coreAngle = 0;
        }

        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle);

        return Mth.lerp(partialTicks, coreAngle, coreAngle + 4f);


    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);

        if (shiptraptionID != NO_SHIPTRAPTION_ID && Math.signum(prevSpeed) != Math.signum(getSpeed()) && prevSpeed != 0) {
//            movedContraption.getContraption()
//                    .stop(level);
        }
        //todo : stop shiptraption
    }

    public float getAngularSpeed() {
        float speed = convertToAngular(isWindmill() ? getGeneratedSpeed() : getSpeed());
        if (getSpeed() == 0)
            speed = 0;
        if (level.isClientSide) {
            speed *= ServerSpeedProvider.get();
            speed += clientAngleDiff / 3f;
        }
        return speed;
    }

    @Override
    public AssemblyException getLastAssemblyException() {
        return lastException;
    }

    protected boolean isWindmill() {
        return false;
    }

    @Override
    public BlockPos getBlockPosition() {
        return worldPosition;
    }

    public void assemble() {
        if (!(level.getBlockState(worldPosition)
                .getBlock() instanceof BearingBlock))
            return;

        Direction direction = getBlockState().getValue(BearingBlock.FACING);
        BlockPos center = worldPosition.relative(direction);
        DenseBlockPosSet selection;

        try {
            selection = GlueAssembler.collectGlued(this.level, center, GlueType.SUPER);
            this.lastException = null;
        } catch (AssemblyException e) {
            lastException = e;
            this.sendData();
            return;
        }



        if (selection == null) return;

        ServerShip shiptraption = ShipAssemblyKt.createNewShipWithBlocks(center, selection, (ServerLevel) level);

//        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        ClockWorkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition);

        shiptraptionID = shiptraption.getId();
        if (level.isClientSide) {
            return;
        }
        //bearing data
        Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
        Vector3dc axis = VectorConversionsMCKt.toJOMLD(direction.getNormal());

        Ship shipOn = VSGameUtilsKt.getShipObjectManagingPos(level, worldPosition);

        long otherShipID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
        if (shipOn != null) {
            otherShipID = shipOn.getId();
        }
        Quaterniond rotationQuaternion;
        switch (direction) {
            case DOWN -> {
                rotationQuaternion = new Quaterniond(new AxisAngle4d(Math.PI, new Vector3d(1.0, 0.0, 0.0)));
            }
            case NORTH -> {
                rotationQuaternion = new Quaterniond(new AxisAngle4d(Math.PI, new Vector3d(0.0, 1.0, 0.0))).mul(new Quaterniond(new AxisAngle4d(Math.PI / 2.0, new Vector3d(1.0, 0.0, 0.0)))).normalize();
            }
            case EAST -> {
                rotationQuaternion = new Quaterniond(new AxisAngle4d(0.5 * Math.PI, new Vector3d(0.0, 1.0, 0.0))).mul(new Quaterniond(new AxisAngle4d(Math.PI / 2.0, new Vector3d(1.0, 0.0, 0.0)))).normalize();
            }
            case SOUTH -> {
                rotationQuaternion = new Quaterniond(new AxisAngle4d(Math.PI / 2.0, new Vector3d(1.0, 0.0, 0.0))).normalize();
            }
            case WEST -> {
                rotationQuaternion = new Quaterniond(new AxisAngle4d(1.5 * Math.PI, new Vector3d(0.0, 1.0, 0.0))).mul(new Quaterniond(new AxisAngle4d(Math.PI / 2.0, new Vector3d(1.0, 0.0, 0.0)))).normalize();
            }
            default -> {
                // UP or null
                rotationQuaternion = new Quaterniond();
            }
        }
        Vector3dc posInOwnerShip = VectorConversionsMCKt.toJOMLD(worldPosition.relative(getBlockState().getValue(BlockStateProperties.FACING), 1)).add(0.5, 0.5, 0.5);
        Vector3dc posInWorld = shiptraption.getTransform().getPositionInWorld(); // posInOwnerShip;
        Quaterniondc rotInWorld = new Quaterniond();
        Vector3dc scaling = new Vector3d(1.0, 1.0, 1.0);

        final int shipChunkX = shiptraption.getChunkClaim().getXMiddle();
        final int shipChunkZ = shiptraption.getChunkClaim().getZMiddle();
        final Vector3dc centerInShip = new Vector3d(
                (shipChunkX << 4) + (center.getX() & 15),
                center.getY(),
                (shipChunkZ << 4) + (center.getZ() & 15)
        );

        if (shipOn != null) {
            scaling = shipOn.getTransform().getShipToWorldScaling();
            final Vector3dc offset = shiptraption.getInertiaData().getCenterOfMassInShip().sub(centerInShip, new Vector3d());

            posInWorld = shipOn.getTransform().getShipToWorld().transformPosition(posInOwnerShip.add(offset, new Vector3d()), new Vector3d());
            rotInWorld = shipOn.getTransform().getShipToWorldRotation();

        }

        ((ShipDataCommon) shiptraption).setTransform(ShipTransformImpl.Companion.create(posInWorld, shiptraption.getInertiaData().getCenterOfMassInShip(), rotInWorld, scaling));

        final Vector3dc bearingPos = centerInShip.add(0.5, 0.5, 0.5, new Vector3d());

        VSAttachmentConstraint constraint = new VSAttachmentConstraint(shiptraptionID, otherShipID, 1e-10, bearingPos, posInOwnerShip, 1e10, 0);
        Quaterniondc hingeOrientation = rotationQuaternion.mul(new Quaterniond(new AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)), new Quaterniond()).normalize();

        VSHingeOrientationConstraint hingeConstraint = new VSHingeOrientationConstraint(shiptraptionID, otherShipID, 1e-8, hingeOrientation, hingeOrientation, 1e10);

        // Add position damping to make the hinge more stable
        // VSPosDampingConstraint posDampingConstraint = new VSPosDampingConstraint(shiptraptionID, otherShipID, 1e-10, posInBearingContraption, posInOwnerShip, 1e10, 1e-2);

        // Add rotation damping to make the hinge more stable
        // VSRotDampingConstraint perpendicularRotDampingConstraint = new VSRotDampingConstraint(shiptraptionID, otherShipID, 1e-10, hingeOrientation, hingeOrientation, 1e10, 1e-2, VSRotDampingAxes.ALL_AXES);


        Integer constraintID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(constraint);
        Integer hingeID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(hingeConstraint);
        // Integer posDamperID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(posDampingConstraint);
        // Integer rotDamperID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(perpendicularRotDampingConstraint);

        VSConstraintAndId contraptionConstraint = new VSConstraintAndId(constraintID, constraint);
        VSConstraintAndId hingeContraptionConstraint = new VSConstraintAndId(hingeID, hingeConstraint);
        // VSConstraintAndId posDampingContraptionConstraint = new VSConstraintAndId(posDamperID, posDampingConstraint);
        // VSConstraintAndId rotDampingContraptionConstraint = new VSConstraintAndId(rotDamperID, perpendicularRotDampingConstraint);

        PhysBearingCreateData data = new PhysBearingCreateData(pos, axis, angle, getSpeed(), (movementMode.get().equals(LockedMode.LOCKED)), shiptraptionID, contraptionConstraint, hingeContraptionConstraint, null, null);

        if (!level.isClientSide) {
            bearingID = PhysBearingController.getOrCreate(shiptraption).addPhysBearing(data);
        }

        running = true;
        angle = 0;
        sendData();
        updateGeneratedRotation();
    }

    @Override
    public void destroy() {
        if (level != null) {
            if (!level.isClientSide) {
                ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);
                if (ship != null) {
                    PhysBearingController controller = PhysBearingController.getOrCreate(ship);

                    Integer attachID = controller.bearingData.get(bearingID).attachID;

                    if (attachID != null) {
                        VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).removeConstraint(attachID);
                    }

                    Integer hingeID = controller.bearingData.get(bearingID).hingeID;

                    if (hingeID != null) {
                        VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).removeConstraint(hingeID);
                    }

                    controller.removePhysBearing(bearingID);
                }
            }
        }
    }

    public void disassemble() {
        if (!running && shiptraptionID == NO_SHIPTRAPTION_ID)
            return;
        angle = 0;
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);
            if (ship != null) {
//
//                PhysBearingController controller = PhysBearingController.getOrCreate(ship);
//                Direction direction = getBlockState().getValue(BearingBlock.FACING);
//                Vector3dc inWorld = VectorConversionsMCKt.toJOMLD(worldPosition.relative(direction, 1));
//                if (!controller.canDisassemble()) {
//                    disassembleWhenPossible = true;
//                    controller.setAligning(true, shiptraptionID);
//                } else {
//                    shipDisassemble();
//                }

//                controller.removePhysBearing(bearingID);
            }

            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition);
            //todo finish disassembly

            return;
        }

        shiptraptionID = NO_SHIPTRAPTION_ID;
        running = false;
        updateGeneratedRotation();
        assembleNextTick = false;
        sendData();
    }

    private void shipDisassemble() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) { return; }
        ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);
        if (ship == null) { return; }
        if (bearingID != null) {
            PhysBearingController controller = PhysBearingController.getOrCreate(ship);
            Direction direction = getBlockState().getValue(BearingBlock.FACING);
            Vector3dc inWorld = VectorConversionsMCKt.toJOMLD(worldPosition.relative(direction, 1));
            if (!controller.canDisassemble()) {
                return;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        prevAngle = angle;
        if (level.isClientSide)
            clientAngleDiff /= 2;

        if (!level.isClientSide && assembleNextTick) {
            assembleNextTick = false;
            if (running) {
//                disassemble();
//                return;
            } else {
                assemble();
            }
        }

        if (shouldRefresh) {
            if (!level.isClientSide) {
                ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);
                if (ship != null && bearingID != null) {
                    PhysBearingData bearingData = PhysBearingController.getOrCreate(ship).bearingData.get(bearingID);
                    if (bearingData != null) {
                        VSAttachmentConstraint attachConstraint1 = bearingData.attachConstraint;
                        VSHingeOrientationConstraint hingeConstraint1 = bearingData.hingeConstraint;

                        //todo TEMP, REPLACE ONCE TRIODE FIXES
                        Ship shipOn = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
                        long shipOnID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId(level));
                        if (shipOn != null) {
                            shipOnID = shipOn.getId();
                        } else {
                            // The ship was deleted, delete this bearing
                            if (VSGameUtilsKt.isBlockInShipyard(level, worldPosition)) {
                                running = false;
                                assembleNextTick = false;
                                shouldRefresh = false;
                                return;
                            }
                        }
                        VSAttachmentConstraint attachConstraint = new VSAttachmentConstraint(attachConstraint1.getShipId0(), shipOnID, attachConstraint1.getCompliance(), attachConstraint1.getLocalPos0(), attachConstraint1.getLocalPos1(), attachConstraint1.getMaxForce(), attachConstraint1.getFixedDistance());
                        VSHingeOrientationConstraint hingeConstraint = new VSHingeOrientationConstraint(hingeConstraint1.getShipId0(), shipOnID, hingeConstraint1.getCompliance(), hingeConstraint1.getLocalRot0(), hingeConstraint1.getLocalRot1(), hingeConstraint1.getMaxTorque());
                        //todo
                        boolean createdAttachment = false;
                        boolean createdHinge = false;

                        if (attachConstraint != null) {
                            Integer attachID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(attachConstraint);
                            if (attachID != null) {
                                PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).attachConstraint = attachConstraint;
                                PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).attachID = attachID;
                                createdAttachment = true;
                            }
                        }
                        if (hingeConstraint != null) {
                            Integer hingeID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(hingeConstraint);
                            if (hingeID != null) {
                                PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeConstraint = hingeConstraint;
                                PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID = hingeID;
                                createdHinge = true;
                            }
                        }

                        if (createdHinge && createdAttachment) {
                            shouldRefresh = false;
                        }

                    }
                }

            }
        }

        if (inOutCorner < 1 && !cornerShrinking) {
            inOutCorner += 0.0075f;
        } else if (inOutCorner >= 1) {
            cornerShrinking = true;
        }

        if (inOutCorner > 0 && cornerShrinking) {
            inOutCorner -= 0.0075f;
        } else if (inOutCorner <= 0) {
            cornerShrinking = false;
        }

        if (running && !open && !opening) {
            opening = true;
        }

        if (opening && running && openProgress < 1) {
            openProgress += 0.05f;
        } else if (openProgress >= 1) {
            opening = false;
            open = true;
            openProgress = 1;
        }

        if (!running)
            return;

        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            float angularSpeed = getAngularSpeed();
            float newAngle = angle + angularSpeed;
            angle = (float) (newAngle % 360);
        }

        if (running) {
            if (!level.isClientSide) {
                if (shiptraptionID != NO_SHIPTRAPTION_ID) {
                    ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);

                    if (ship != null) {
                        //todo add locked mode

                        if (PhysBearingController.getOrCreate(ship).bearingData.get(bearingID) == null) {
                            return;
                        }

                        VSHingeOrientationConstraint hingeConstraint = PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeConstraint;
                        VSFixedOrientationConstraint angleConstraint = PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).angleConstraint;
//                        if (PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID == null) {
//                            return;
//                        }
//                        if (movementMode.get() == LockedMode.LOCKED) {
//                            Vector3dc facing = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(BlockStateProperties.FACING).getNormal());
//                            Quaterniond localRot0 = new Quaterniond(hingeConstraint.getLocalRot0());
//                            localRot0 = localRot0.premul(new Quaterniond(new AxisAngle4d(Math.toRadians(angle), facing))).normalize();
//                            angleConstraint = new VSFixedOrientationConstraint(hingeConstraint.getShipId0(), hingeConstraint.getShipId1(), 1e-10, localRot0, hingeConstraint.getLocalRot1(), 1e8);
//                            VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).updateConstraint(PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID, angleConstraint);
//                        } else if (movementMode.get() == LockedMode.UNLOCKED) {
//                            hingeConstraint = PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeConstraint;
//                            VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).updateConstraint(PhysBearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID, hingeConstraint);
//
//                        }

                        PhysBearingUpdateData data = new PhysBearingUpdateData(angle, getSpeed(), (movementMode.get().equals(LockedMode.LOCKED)), null, null);
                        PhysBearingController.getOrCreate(ship).updatePhysBearing(bearingID, data);
                    }
                }
            }
        }

        if (disassembleWhenPossible) {
            shipDisassemble();
        }



        applyRotation();
    }

    public boolean isNearInitialAngle() {
        return Math.abs(angle) < 45 || Math.abs(angle) > 7 * 45;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level.isClientSide)
            sendData();
    }

    protected void applyRotation() {

    }

    @Override
    public void attach(ControlledContraptionEntity contraption) {
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
    public boolean isAttachedTo(AbstractContraptionEntity contraption) {
        return false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (super.addToTooltip(tooltip, isPlayerSneaking))
            return true;
        if (isPlayerSneaking)
            return false;
        if (!isWindmill() && getSpeed() == 0)
            return false;
        if (running)
            return false;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof BearingBlock))
            return false;

        BlockState attachedState = level.getBlockState(worldPosition.relative(state.getValue(BearingBlock.FACING)));
        if (attachedState.getMaterial()
                .isReplaceable())
            return false;
        TooltipHelper.addHint(tooltip, "hint.empty_bearing");
        return true;
    }

    public void setAngle(float forcedAngle) {
        angle = forcedAngle;
    }
    @Override
    public boolean isShipContraptionController() {
        return true;
    }

    @Nullable
    @Override
    public Ship getConnectedShip() {
        return null;
    }

    public float getAngle() {
        return this.angle;
    }
}
