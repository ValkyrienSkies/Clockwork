package org.valkyrienskies.clockwork.mixin.content.fan;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanCreateData;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanUpdateData;
import org.valkyrienskies.clockwork.content.forces.EncasedFanController;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(EncasedFanBlockEntity.class)
public abstract class MixinEncasedFanTileEntity extends KineticBlockEntity {

//    @Shadow(remap = false)
//    LerpedFloat visualSpeed;

    private Integer fanID = null;
    private boolean alreadyAdded = false;

    public MixinEncasedFanTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
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
            if (!alreadyAdded && fanID == null) {
                Vector3dc pos = VectorConversionsMCKt.toJOMLD(worldPosition);
                Vector3dc axis = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(BlockStateProperties.FACING).getNormal());
                final EncasedFanCreateData data = new EncasedFanCreateData(pos, axis, speed);
                fanID = EncasedFanController.Companion.getOrCreate(ship).addEncasedFan(data);
                alreadyAdded = true;
            }
            if (alreadyAdded && fanID != null) {
                final EncasedFanUpdateData data = new EncasedFanUpdateData(speed);
                EncasedFanController.Companion.getOrCreate(ship).updateEncasedFan(fanID, data);
            }
            if (this.isRemoved()) {
                if (fanID != null) {
                    EncasedFanController.Companion.getOrCreate(ship).removeEncasedFan(fanID);
                    fanID = null;
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
        if (fanID != null) {
            compound.putInt("fanID", fanID);
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
        if (compound.contains("fanID")) {
            fanID = compound.getInt("fanID");
        }
        return compound;
    }
    @Inject(method = "read", at = @At("HEAD"), cancellable = true, remap = false)
    private void injectRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        compound = readFromCompound(compound,clientPacket);
        super.read(compound, clientPacket);
        ci.cancel();
    }
}
