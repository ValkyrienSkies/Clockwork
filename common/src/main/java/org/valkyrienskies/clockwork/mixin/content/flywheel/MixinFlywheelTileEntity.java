package org.valkyrienskies.clockwork.mixin.content.flywheel;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.contraptions.flywheel.FlywheelCreateData;
import org.valkyrienskies.clockwork.content.contraptions.flywheel.FlywheelUpdateData;
import org.valkyrienskies.clockwork.content.forces.FlywheelController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(FlywheelTileEntity.class)
public abstract class MixinFlywheelTileEntity extends KineticTileEntity {

    @Shadow(remap = false)
    LerpedFloat visualSpeed;

    private Integer fwID = null;
    private boolean alreadyAdded = false;

    public MixinFlywheelTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Unique
    private void handleController() {
        //do stuff here (I like to have the inject call to a separate function so the breakpoint will work properly for this function)

        LoadedServerShip ship = null;
        if (!level.isClientSide) {
            if (VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos()) != null) {
                ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
            }
        }
        if (ship != null) {
            if (!alreadyAdded && fwID == null) {
                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);

                Vector3dc axis = switch (getBlockState().getValue(BlockStateProperties.AXIS)) {
                    case X -> new Vector3d(1, 0, 0);
                    case Y -> new Vector3d(0, 1, 0);
                    case Z -> new Vector3d(0, 0, 1);
                };
                final FlywheelCreateData data = new FlywheelCreateData(pos, axis, visualSpeed, speed);
                fwID = FlywheelController.getOrCreate(ship).addFlywheel(data);
                alreadyAdded = true;
            }
            if (alreadyAdded && fwID != null) {
                final FlywheelUpdateData data = new FlywheelUpdateData(visualSpeed, speed);
                FlywheelController.getOrCreate(ship).updateFlywheel(fwID, data);
            }
            if (this.isRemoved()) {
                if (fwID != null) {
                    FlywheelController.getOrCreate(ship).removeFlywheel(fwID);
                    fwID = null;
                    alreadyAdded = false;
                }
            }
        }
    }
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void injectTick(CallbackInfo ci) {
        handleController();
    }

    @Unique
    private CompoundTag writeToCompound(CompoundTag compound, boolean clientPacket){
        //write here
        compound.putBoolean("alreadyAdded", alreadyAdded);
        if (fwID != null) {
            compound.putInt("fwID", fwID);
        }
        return compound;
    }
    @Inject(method = "write", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectWrite(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        compound = writeToCompound(compound,clientPacket);
        super.write(compound, clientPacket);
        ci.cancel();
    }

    @Unique
    private CompoundTag readFromCompound(CompoundTag compound, boolean clientPacket){
        //read (and remove before it passes up?) here
        alreadyAdded = compound.getBoolean("alreadyAdded");
        if (compound.contains("fwID")) {
            fwID = compound.getInt("fwID");
        }
        return compound;
    }
    @Inject(method = "read", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        compound = readFromCompound(compound,clientPacket);
        super.read(compound, clientPacket);
        if (clientPacket)
            visualSpeed.chase(getGeneratedSpeed(), 1 / 64f, LerpedFloat.Chaser.EXP);
        ci.cancel();
    }
}