package org.valkyrienskies.clockwork.content.contraptions.reaction_wheel;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.AfterblazerController;
import org.valkyrienskies.clockwork.content.forces.ReactionWheelController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class ReactionWheelBlockEntity extends KineticTileEntity {
    float angle = 0;
    float rotspeed = 0;
    boolean active;
    boolean wasActive = false;
    boolean alreadyAdded = false;
    boolean spinup = false;
    boolean spindown = false;
    float spinupProg = 0;
    float spindownProg = 0;
    float activeControlSpeed = 0;
    boolean activeControlMode;

    boolean shouldRemove = false;

    Integer rwID = null;

    public ReactionWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setShouldRemove() {
        this.shouldRemove = true;
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox().inflate(4);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.putFloat("angle", angle);
        compound.putFloat("rotspeed", rotspeed);
        compound.putBoolean("active", active);
        compound.putBoolean("wasActive", wasActive);
        compound.putBoolean("alreadyAdded", alreadyAdded);
        if (rwID != null) {
            compound.putInt("rwID", rwID);
        }
        super.write(compound, clientPacket);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        angle = compound.getFloat("angle");
        rotspeed = compound.getFloat("rotspeed");
        active = compound.getBoolean("active");
        wasActive = compound.getBoolean("wasActive");
        alreadyAdded = compound.getBoolean("alreadyAdded");
        if (compound.contains("rwID")) {
            rwID = compound.getInt("rwID");
        }

        super.read(compound, clientPacket);

    }

    @Override
    public void remove() {
        if (level != null) {
            if (!level.isClientSide) {
                ServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, worldPosition);
                if (ship != null) {
                    ReactionWheelController controller = ReactionWheelController.getOrCreate(ship);

                    controller.removeReactionWheel(rwID);
                }
            }
        }
        super.remove();
    }

    @Override
    public void tick() {
        super.tick();

        activeControlMode = true;

        if (active) {
            if (!wasActive && !activeControlMode) {
                spindown = false;
                spinup = true;
                spinupProg = speed - rotspeed;
            }
            if (spinup) {
                modSpinupSpeed();
            } else {
                modSpeed();
            }
        } else {
            if (wasActive) {
                spinup = false;
                spindown = true;
                spindownProg = rotspeed;
            }
            if (spindown) {
                modSpindownSpeed();
            } else {
                rotspeed = 0;
            }
        }

        angle += rotspeed * 3 / 10f;
        angle %= 360;

        LoadedServerShip ship = null;
        if (!level.isClientSide) {
            if (VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos()) != null) {
                ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
            }
        }
        wasActive = active;
        if (ship != null) {
            if (!alreadyAdded && rwID == null || ReactionWheelController.getOrCreate(ship).checkReactionWheel(rwID)) {
                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);

                Vector3dc axis = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
                    case X -> new Vector3d(1, 0, 0);
                    case Y -> new Vector3d(0, 1, 0);
                    case Z -> new Vector3d(0, 0, 1);
                };
                final ReactionWheelCreateData data = new ReactionWheelCreateData(pos, axis, rotspeed, spinup, spindown, active, speed);
                rwID = ReactionWheelController.getOrCreate(ship).addReactionWheel(data);
                alreadyAdded = true;
            }
            if (alreadyAdded && rwID != null) {

                if (ReactionWheelController.getOrCreate(ship) != null) {
                    final ReactionWheelUpdateData data = new ReactionWheelUpdateData(rotspeed, speed);
                    ReactionWheelController.getOrCreate(ship).updateReactionWheel(rwID, data);
//                active = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
//                    case X -> Math.abs(ship.getOmega().x()) >= 10;
//                    case Y -> Math.abs(ship.getOmega().y()) >= 10;
//                    case Z -> Math.abs(ship.getOmega().z()) >= 10;
//                };
                    //FOR TESTING
                    Vector3dc axis = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
                        case X -> new Vector3d(1, 0, 0);
                        case Y -> new Vector3d(0, 1, 0);
                        case Z -> new Vector3d(0, 0, 1);
                    };
                    active = true;
                    if (active && activeControlMode) {
                        if (Float.isNaN(rotspeed)) {
                            rotspeed = 0;
                        }
                        activeControlSpeed = (float) computeTargetSpeed(ship, rotspeed, axis);
                        if (Math.abs(activeControlSpeed) > Math.abs(speed)) {
                            activeControlSpeed = speed;
                        }
                    }
                }
            }

            if (this.isRemoved() || shouldRemove) {
                if (rwID != null) {
                    ReactionWheelController.getOrCreate(ship).removeReactionWheel(rwID);
                    rwID = null;
                    alreadyAdded = false;
                }
            }

        }
    }

    private double computeTargetSpeed(LoadedServerShip ship, float rspeed, Vector3dc axis) {
        double wheelSpeed = rspeed;
        double wheelMass = 18000;

        double wheelInertiaD = (0.5 * wheelMass) * (Math.pow(0.25, 2) + Math.pow(0.75, 2));
        double wheelOmegaD = wheelSpeed * ((2 * Math.PI) / 20);

        Vector3dc wheelOmega = new Vector3d(axis).mul(wheelOmegaD);
        Vector3dc wheelInertia = new Vector3d(axis).mul(wheelInertiaD);

        Vector3dc shipOmega = ship.getOmega();
        Matrix3dc shipInertia = ship.getInertiaData().getMomentOfInertiaTensor();

        Vector3d wheelL = wheelOmega.mul(wheelInertia, new Vector3d());

        Vector3d shipL = shipOmega.mul(shipInertia, new Vector3d());

        Vector3dc Lt = wheelL.add(shipL, new Vector3d());

        Vector3d targetWheelL = Lt.div(wheelInertia.length(), new Vector3d());

//        Vector3d targetWheelOmega = targetWheelL.div(wheelInertiaD, new Vector3d());

        double targetWheelSpeed = targetWheelL.length() / ((2 * Math.PI) / 20);

        return targetWheelSpeed;
    }

    private void modSpeed() {
        float targetSpeed = speed;
        if (activeControlMode) {
            targetSpeed = activeControlSpeed;
        }

        if (rotspeed == targetSpeed) {
            return;
        }
//        if ((int) getSourceSpeed() == 0 && (int) speed == 0) {
//            speed = 0;
//            return;
//        }


        float diff = targetSpeed - rotspeed;
        rotspeed = rotspeed + Mth.clamp(diff / 10, -32, 32);
//        float delta = Mth.clamp(, lastSpeed, 10)
        //rotspeed = (float) Mth.lerp(delta, lastSpeed, targetSpeed);
    }

    private void modSpindownSpeed() {
        spindownProg--;
        if (spindownProg <= 0) {
            spindown = false;
            return;
        }

        float stoppingPoint = (angle + rotspeed * spindownProg * 0.5f);
        float optimalStoppingPoint = 90f * Math.round(stoppingPoint / 90f);
        float Q = (optimalStoppingPoint - stoppingPoint) / spindownProg;
        rotspeed = (rotspeed + 6f * Q / spindownProg) * (1f - 1f / spindownProg);
    }

    private void modSpinupSpeed() {
        if (level.isClientSide) {
            return;
        }

        spinupProg--;
        if (Math.abs(rotspeed) >= Math.abs(speed)) {
            spinup = false;
            if (Math.abs(rotspeed) > Math.abs(speed)) {
                rotspeed = speed;
            }
            return;
        }

//            float time = 1f - (spinup / 20f);
//            float Q = (rotspeed + (targetSpeed - rotspeed)) * time;
        float startingPoint = (angle + speed * spinupProg * 0.5f);
        float Q = (startingPoint) / spinupProg;
        rotspeed = (rotspeed + 6f * Q / spinupProg) * (1f - 1f / spinupProg);
    }
}
