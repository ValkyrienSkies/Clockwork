package org.valkyrienskies.clockwork.mixin;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.flywheel.FlywheelTileEntity;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlywheelTileEntity.class)
public abstract class MixinFlywheelTileEntity extends KineticTileEntity {

    @Shadow(remap = false)
    LerpedFloat visualSpeed;

    public MixinFlywheelTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Unique
    private void doStuff() {
        //do stuff here (I like to have the inject call to a separate function so the breakpoint will work properly for this function)
    }
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void injectTick(CallbackInfo ci) {
        doStuff();
    }

    @Unique
    private CompoundTag writeToCompound(CompoundTag compound, boolean clientPacket){
        //write here
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
