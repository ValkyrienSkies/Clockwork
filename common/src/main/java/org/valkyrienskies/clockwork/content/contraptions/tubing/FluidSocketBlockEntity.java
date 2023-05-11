package org.valkyrienskies.clockwork.content.contraptions.tubing;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.ITransformableTE;
import com.simibubi.create.content.contraptions.fluids.*;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.tileEntity.SmartTileEntity;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.Couple;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.FluidSocketController;
import org.valkyrienskies.clockwork.util.fluid.CWFluidTankBehaviour;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class FluidSocketBlockEntity extends KineticTileEntity implements IHaveGoggleInformation {

    LerpedFloat arrowDirection;
    Couple<MutableBoolean> sidesToUpdate;
    boolean pressureUpdate;
    boolean reversed;

    private BlockPos connectedPos;
    private Integer fluidSocketID;

    public CWFluidTankBehaviour tank;

    private boolean master = false;

    private boolean refresh = false;

    public FluidSocketBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        arrowDirection = LerpedFloat.linear()
                .startWithValue(1);
        sidesToUpdate = Couple.create(MutableBoolean::new);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        tank = CWFluidTankBehaviour.single(this, 1000);
        registerAwardables(behaviours, FluidPropagator.getSharedTriggers());
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        tooltip.add(new TextComponent(spacing).append(new TextComponent("Transferring/Receiving").withStyle(ChatFormatting.GRAY)));
        tooltip.add(new TextComponent(spacing).append(new TextComponent(" " + getTransferRate() + "mb/t ")
                .withStyle(ChatFormatting.AQUA)).append(new TextComponent("with current fuel").withStyle(ChatFormatting.DARK_GRAY)));
        return true;
    }

    public float getTransferRate() {
        if (connectedSocket() != null) {
            int invertedMul = 1;
            int otherInvertedMul = 1;
            if (reversed) {
                invertedMul = -1;
            }
            if (connectedSocket().reversed) {
                otherInvertedMul = -1;
            }
            if (Math.abs(connectedSocket().getSpeed()) > Math.abs(this.getSpeed())) {
                return (Math.abs(connectedSocket().getSpeed()) * invertedMul) + this.getSpeed();
            }
        }
        return 0f;
    }

    @Nullable
    public FluidSocketBlockEntity connectedSocket() {
        if (connectedPos != null && level != null && level.getBlockEntity(connectedPos) != null) {
            if (level.getBlockEntity(connectedPos) instanceof FluidSocketBlockEntity) {
                return (FluidSocketBlockEntity) level.getBlockEntity(connectedPos);
            }
        }
        return null;
    }

    public void setConnectedPos(BlockPos pos, boolean master) {
        this.connectedPos = pos;
        this.master = master;
        refresh = true;
    }

    public BlockPos getConnectedPos() {
        return connectedPos;
    }

    public boolean isMaster() {
        return master;
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if (compound.contains("connectedX") && compound.contains("connectedY") && compound.contains("connectedZ")) {
            connectedPos = new BlockPos(compound.getInt("connectedX"), compound.getInt("connectedY"), compound.getInt("connectedZ"));
        }
        master = compound.getBoolean("master");
        if (compound.contains("fluidSocketID")) {
            fluidSocketID = compound.getInt("fluidSocketID");
        }

        refresh = true;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket) {

        if (connectedPos != null) {
            compound.putInt("connectedX", connectedPos.getX());
            compound.putInt("connectedY", connectedPos.getY());
            compound.putInt("connectedZ", connectedPos.getZ());
        }
        compound.putBoolean("master", master);
        if (fluidSocketID != null) {
            compound.putInt("fluidSocketID", fluidSocketID);
        }
        super.write(compound, clientPacket);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (this.connectedPos != null) {

            if (refresh) {
                if (fluidSocketID != null) {
                    ServerShip otherShip = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, connectedPos);
                    if (otherShip != null) {
                        if (FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID) != null) {
                            VSRopeConstraint constraint = FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraint;
                            ServerShip shipOn = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
                            if (shipOn == null) {
                                //todo TEMP REMOVE ONCE TRIODE FIXES WORLD ID
                                long worldID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId((ServerLevel) level));
                                constraint = new VSRopeConstraint(worldID, constraint.getShipId1(), constraint.getCompliance(), constraint.getLocalPos0(), constraint.getLocalPos1(), constraint.getMaxForce(), constraint.getRopeLength());
                            }
                            if (constraint != null) {
                                Integer constraintID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(constraint);
                                if (constraintID != null) {
                                    FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraintID = constraintID;
                                } else {
                                    return;
                                }
                            }
                        }
                    }
                }

                refresh = false;
            }

            if (!master) {
                return;
            }
            ServerShip ship = (ServerShip) VSGameUtilsKt.getShipObjectManagingPos(level, worldPosition);
            ServerShip otherShip = (ServerShip) VSGameUtilsKt.getShipObjectManagingPos(level, connectedPos);

            if (otherShip == null) {
                return;
            }

            if (fluidSocketID == null) {
                FluidSocketCreateData data = new FluidSocketCreateData(VectorConversionsMCKt.toJOMLD(connectedPos));
                fluidSocketID = FluidSocketController.getOrCreate(otherShip).addFluidSocket(data);
                return;
            } else {

                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition).add(0.5, 0.5, 0.5);
                Vector3dc otherPos = VectorConversionsMCKt.toJOMLD(connectedPos).add(0.5, 0.5, 0.5);

                if (FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID) == null) {
                    return;
                }

                Vector3dc posInWorld = pos;
                long shipID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).getDimensionToGroundBodyIdImmutable().get(VSGameUtilsKt.getDimensionId((ServerLevel) level));
                if (ship != null) {
                    if (ship.getId() == otherShip.getId()) {
                        return;
                    }
                    posInWorld = ship.getTransform().getShipToWorld().transformPosition(pos, new Vector3d());
                    shipID = ship.getId();
                }

                Vector3dc otherPosInWorld = otherShip.getTransform().getShipToWorld().transformPosition(otherPos, new Vector3d());

                double distance = posInWorld.distance(otherPosInWorld);
                distance = Mth.clamp(distance, 1, 12);

                VSRopeConstraint ropeConstraint = new VSRopeConstraint(shipID, otherShip.getId(), 1e-10, pos, otherPos, 1e6, 4);

                if (FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraint != null) {
                    ropeConstraint = FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraint;
                }

                Integer ropeID;
                if (FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraintID != null) {
                    ropeID = FluidSocketController.getOrCreate(otherShip).socketData.get(fluidSocketID).constraintID;
                } else {
                    ropeID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(ropeConstraint);
                }


                FluidSocketUpdateData data = new FluidSocketUpdateData(VectorConversionsMCKt.toJOMLD(connectedPos), ropeConstraint, ropeID);

                FluidSocketController.getOrCreate(otherShip).updateFluidSocket(fluidSocketID, data);
            }

        }
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide && connectedPos != null) {
            ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, connectedPos);
            if (fluidSocketID != null && ship != null) {
                Integer constraintID = FluidSocketController.getOrCreate(ship).socketData.get(fluidSocketID).constraintID;

                if (constraintID != null) {
                    VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).removeConstraint(constraintID);
                }
                FluidSocketController.getOrCreate(ship).removeFluidSocket(fluidSocketID);
            }
            FluidSocketBlockEntity ube = level.getBlockEntity(connectedPos) instanceof FluidSocketBlockEntity ? (FluidSocketBlockEntity) level.getBlockEntity(connectedPos) : null;

            if (ube != null) {
                ube.setConnectedPos(null, false);
            }
        }
        super.remove();
    }
}
