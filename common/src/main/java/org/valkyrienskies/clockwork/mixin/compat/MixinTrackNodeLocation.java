package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.logistics.trains.TrackNodeLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackNodeLocation.class)
abstract class MixinTrackNodeLocation extends Vec3i {
    public MixinTrackNodeLocation(int x, int y, int z) {
        super(x, y, z);
    }

    public MixinTrackNodeLocation(double x, double y, double z) {
        super(x, y, z);
    }

    /**
     * This method overwrites getLocation to make it actually parse things as a double, not needed in forge create
     */
    @Inject(
            method = "getLocation", at = @At("HEAD"), cancellable = true
    )
    protected void getLocation(final CallbackInfoReturnable<Vec3> cir) {

        cir.setReturnValue(new Vec3((double) this.getX() / 2, (double) this.getY() / 2, (double) this.getZ() / 2));
    }

    @Redirect(
            method = "receive",
            at = @At(value="INVOKE",target = "Lnet/minecraft/network/FriendlyByteBuf;readBlockPos()Lnet/minecraft/core/BlockPos;")
    )
    private static BlockPos redirectReadBlockPos(FriendlyByteBuf instance) {
        final double x = instance.readInt();
        final double y = instance.readInt();
        final double z = instance.readInt();
        return new BlockPos(x, y, z);
    }

    @Redirect(
            method = "send",
            at = @At(value="INVOKE",target = "Lnet/minecraft/network/FriendlyByteBuf;writeBlockPos(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/network/FriendlyByteBuf;")
    )
    private FriendlyByteBuf redirectWriteBlockPos(FriendlyByteBuf instance, BlockPos pos) {
        instance.writeInt(pos.getX());
        instance.writeInt(pos.getY());
        instance.writeInt(pos.getZ());
        return instance;
    }
}
