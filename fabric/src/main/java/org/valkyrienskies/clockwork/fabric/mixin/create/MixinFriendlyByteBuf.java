package org.valkyrienskies.clockwork.fabric.mixin.create;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FriendlyByteBuf.class)
public abstract class MixinFriendlyByteBuf {
    @Inject(
        method = "readBlockPos",
        at = @At("HEAD"), cancellable = true
    )
    private void redirectReadBlockPos(final CallbackInfoReturnable<BlockPos> cir) {
        final double x = ((FriendlyByteBuf) (Object) this).readDouble();
        final double y = ((FriendlyByteBuf) (Object) this).readDouble();
        final double z = ((FriendlyByteBuf) (Object) this).readDouble();
        cir.setReturnValue(new BlockPos(x, y, z));
    }

    @Inject(
        method = "writeBlockPos",
        at = @At("HEAD"), cancellable = true
    )
    private void redirectWriteBlockPos(final BlockPos blockPos, final CallbackInfoReturnable<FriendlyByteBuf> cir) {
        ((FriendlyByteBuf) (Object) this).writeDouble(blockPos.getX());
        ((FriendlyByteBuf) (Object) this).writeDouble(blockPos.getY());
        ((FriendlyByteBuf) (Object) this).writeDouble(blockPos.getZ());
        cir.setReturnValue(((FriendlyByteBuf) (Object) this));
    }
}