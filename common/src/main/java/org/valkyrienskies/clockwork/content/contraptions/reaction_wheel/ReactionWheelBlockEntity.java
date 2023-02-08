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
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.clockwork.content.forces.ReactionWheelController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
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

    Integer rwID = null;

    public ReactionWheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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
    public void tick() {
        super.tick();

        if (active) {
            if (!wasActive) {
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
            if (!alreadyAdded && rwID == null) {
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
                final ReactionWheelUpdateData data = new ReactionWheelUpdateData(rotspeed, speed);
                ReactionWheelController.getOrCreate(ship).updateReactionWheel(rwID, data);
//                active = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
//                    case X -> Math.abs(ship.getOmega().x()) >= 10;
//                    case Y -> Math.abs(ship.getOmega().y()) >= 10;
//                    case Z -> Math.abs(ship.getOmega().z()) >= 10;
//                };
                //FOR TESTING
                active = true;
            }
            if (this.isRemoved()) {
                if (rwID != null) {
                    ReactionWheelController.getOrCreate(ship).removeReactionWheel(rwID);
                    rwID = null;
                    alreadyAdded = false;
                }
            }

        }
    }

    private void modSpeed() {
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
