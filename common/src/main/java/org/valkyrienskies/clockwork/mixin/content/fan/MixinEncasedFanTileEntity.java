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
import org.valkyrienskies.clockwork.content.forces.EncasedFanController;
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanCreateData;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanData;
import org.valkyrienskies.clockwork.content.propulsion.singleton.fan.EncasedFanUpdateData;
import org.valkyrienskies.clockwork.util.ClockworkConstants;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

@Mixin(EncasedFanBlockEntity.class)
public abstract class MixinEncasedFanTileEntity extends KineticBlockEntity implements IForceApplierBE<EncasedFanUpdateData, EncasedFanData, EncasedFanCreateData, EncasedFanController> {

//    @Shadow(remap = false)
//    LerpedFloat visualSpeed;

    public MixinEncasedFanTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Unique
    private void vs_clockwork$handleController() {
        //do stuff here (I like to have the inject call to a separate function so the breakpoint will work properly for this function)

        LoadedServerShip ship = null;
        if (!level.isClientSide) {
            if (VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos()) != null) {
                ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
            }
        }
        if (ship != null) {
            EncasedFanController attachment = EncasedFanController.Companion.getOrCreate(ship);
            if (attachment != null) {
                tickData(attachment, true);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void vs_clockwork$injectTick(CallbackInfo ci) {
        vs_clockwork$handleController();
    }

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void vs_clockwork$injectWrite(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        if (getPhysID() != -1.0) {
            compound.putInt(ClockworkConstants.Nbt.FAN_ID, getPhysID());
        }
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void vs_clockwork$injectRead(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        if (compound.contains(ClockworkConstants.Nbt.FAN_ID)) {
            setPhysID(compound.getInt(ClockworkConstants.Nbt.FAN_ID));
        }
    }


    @Inject(method = "remove", at = @At("HEAD"), remap = false)
    private void vs_clockwork$injectRemove(CallbackInfo ci) {
        if (level == null || level.isClientSide) {return;}
        if (VSGameUtilsKt.getShipObjectManagingPos(level, getBlockPos()) == null) {return;}
        LoadedServerShip ship = VSGameUtilsKt.getShipObjectManagingPos((ServerLevel) level, getBlockPos());
        if (ship == null) {return;}
        removeApplier(EncasedFanController.class, level, getBlockPos());
    }
}
