package org.valkyrienskies.clockwork.content.contraptions.universal_joint;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.UniversalJointController;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint;
import org.valkyrienskies.core.apigame.constraints.VSRopeConstraint;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class UniversalJointBlockEntity extends KineticTileEntity {

    private BlockPos connectedPos;
    private Integer universalJointId;
    private boolean master = false;

    private boolean refresh = false;

    public UniversalJointBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
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
        if (compound.contains("universalJointId")) {
            universalJointId = compound.getInt("universalJointId");
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
        if (universalJointId != null) {
            compound.putInt("universalJointId", universalJointId);
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
                detachKinetics();

                if (universalJointId != null) {
                    ServerShip otherShip = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, connectedPos);
                    if (otherShip != null) {
                        if (UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId) != null) {
                            VSRopeConstraint constraint = UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraint;
                            if (constraint != null) {
                                Integer constraintID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(constraint);
                                if (constraintID != null) {
                                    UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraintID = constraintID;
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

            if (universalJointId == null) {
                UniversalJointCreateData data = new UniversalJointCreateData(VectorConversionsMCKt.toJOMLD(connectedPos));
                universalJointId = UniversalJointController.getOrCreate(otherShip).addUniversalJoint(data);
                return;
            } else {

                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
                Vector3dc otherPos = VectorConversionsMCKt.toJOMLD(connectedPos);

                if (UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId) == null) {
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
                distance = Mth.clamp(distance, 1, 4);

                VSRopeConstraint ropeConstraint = new VSRopeConstraint(shipID, otherShip.getId(), 1e-10, pos, otherPos, 1e6, 4);

                if (UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraint != null) {
                    ropeConstraint = UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraint;
                }

                Integer ropeID;
                if (UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraintID != null) {
                    ropeID = UniversalJointController.getOrCreate(otherShip).jointData.get(universalJointId).constraintID;
                } else {
                    ropeID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(ropeConstraint);
                }


                UniversalJointUpdateData data = new UniversalJointUpdateData(VectorConversionsMCKt.toJOMLD(connectedPos), ropeConstraint, ropeID);

                UniversalJointController.getOrCreate(otherShip).updateUniversalJoint(universalJointId, data);
            }

        }
    }

    @Override
    public void remove() {
        if (level != null && !level.isClientSide) {
            ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
            if (universalJointId != null) {
                Integer constraintID = UniversalJointController.getOrCreate(ship).jointData.get(universalJointId).constraintID;

                if (constraintID != null) {
                    VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).removeConstraint(constraintID);
                }
                UniversalJointController.getOrCreate(ship).removeUniversalJoint(universalJointId);
            }
            UniversalJointBlockEntity ube = level.getBlockEntity(connectedPos) instanceof UniversalJointBlockEntity ? (UniversalJointBlockEntity) level.getBlockEntity(connectedPos) : null;

            if (ube != null) {
                ube.setConnectedPos(null, false);
            }
        }
        super.remove();
    }
}
