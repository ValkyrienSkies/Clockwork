package org.valkyrienskies.clockwork.content.contraptions.phys.bearing;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.AssemblyException;
import com.simibubi.create.content.contraptions.components.structureMovement.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.components.structureMovement.bearing.IBearingTileEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.PropellorController;
import org.valkyrienskies.clockwork.content.forces.physContraption.PhysBearingController;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;
import org.valkyrienskies.clockwork.platform.api.GlueType;
import org.valkyrienskies.clockwork.util.assemble.GlueAssembler;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.assembly.ShipAssemblyKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;

public class PhysBearingBlockEntity extends GeneratingKineticTileEntity implements IBearingTileEntity, IDisplayAssemblyExceptions, ContraptionController {
    protected ScrollOptionBehaviour<RotationMode> movementMode;
    protected float angle;
    protected boolean running;
    protected boolean assembleNextTick;
    protected float clientAngleDiff;
    protected AssemblyException lastException;
    protected boolean disassembleWhenPossible = false;

    private float prevAngle;

    private long shiptraptionID = -1;

    private Integer bearingID = null;

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
        movementMode = new ScrollOptionBehaviour<>(RotationMode.class, new TextComponent("Locked or Unlocked"),
                this, getMovementModeSlot());
        movementMode.requiresWrench();
        behaviours.add(movementMode);
    }

    @Override
    public void remove() {
        if (!level.isClientSide)
            disassemble();
        super.remove();
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putBoolean("Running", running);
        compound.putFloat("Angle", angle);
        if (bearingID != null) {
            compound.putInt("BearingID", bearingID);
        }
        if (shiptraptionID != -1) {
            compound.putLong("ShiptraptionID", shiptraptionID);
        }
        AssemblyException.write(compound, lastException);
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        if (wasMoved) {
            super.read(compound, clientPacket);
            return;
        }

        float angleBefore = angle;
        running = compound.getBoolean("Running");
        angle = compound.getFloat("Angle");
        lastException = AssemblyException.read(compound);
        super.read(compound, clientPacket);
        if (compound.contains("BearingID")) {
            bearingID = compound.getInt("BearingID");
        }
        if (!clientPacket)
            return;

        if (compound.contains("ShiptraptionID")) {
            shiptraptionID = compound.getLong("ShiptraptionID");
        }
        if (running) {
            if (shiptraptionID == -1) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore, angle);
                angle = angleBefore;
            }
        } else
            shiptraptionID = -1;
    }

    @Override
    public float getInterpolatedAngle(float partialTicks) {
        if (isVirtual())
            return Mth.lerp(partialTicks + .5f, prevAngle, angle);
        if (shiptraptionID == -1 || !running)
            partialTicks = 0;
        return Mth.lerp(partialTicks, angle, angle + getAngularSpeed());
    }

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        assembleNextTick = true;

        if (shiptraptionID != -1 && Math.signum(prevSpeed) != Math.signum(getSpeed()) && prevSpeed != 0) {
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

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);

        shiptraptionID = shiptraption.getId();

        //bearing data
        Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
        Vector3dc axis = VectorConversionsMCKt.toJOMLD(direction.getNormal());
        boolean locked = movementMode.getValue() == 2;
        PhysBearingCreateData data = new PhysBearingCreateData(pos, axis, angle, getSpeed(), true, shiptraptionID);

        if (!level.isClientSide) {
            bearingID = PhysBearingController.getOrCreate(shiptraption).addPhysBearing(data);
        }

        running = true;
        angle = 0;
        sendData();
        updateGeneratedRotation();
    }

    public void disassemble() {
        if (!running && shiptraptionID == -1)
            return;
        angle = 0;
        if (shiptraptionID != -1) {
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

        shiptraptionID = -1;
        running = false;
        updateGeneratedRotation();
        assembleNextTick = false;
        sendData();
    }

    private void shipDisassemble() {
        if (shiptraptionID == -1) { return; }
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
                disassemble();
                return;
            } else {
                assemble();
            }
        }

        if (running) {
            if (shiptraptionID != -1) {
                ServerShip ship = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getAllShips().getById(shiptraptionID);

                if (ship != null) {
                    PhysBearingUpdateData data = new PhysBearingUpdateData(angle, getSpeed(), true);
                    PhysBearingController.getOrCreate(ship).updatePhysBearing(bearingID, data);
                }
            }
        }

        if (disassembleWhenPossible) {
            shipDisassemble();
        }

        if (!running)
            return;

        if (!(shiptraptionID != -1)) {
            float angularSpeed = getAngularSpeed();
            float newAngle = angle + angularSpeed;
            angle = (float) (newAngle % 360);
        }

        applyRotation();
    }

    public boolean isNearInitialAngle() {
        return Math.abs(angle) < 45 || Math.abs(angle) > 7 * 45;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (shiptraptionID != -1 && !level.isClientSide)
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
}
