package org.valkyrienskies.clockwork.forge.mixin.create;

import com.simibubi.create.content.logistics.trains.TrackNodeLocation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrackNodeLocation.class)
public abstract class MixinTrackNodeLocation {
    /**
     * This method overwrites getLocation to make it actually parse things as a double, not needed in forge create
     */
    @Inject(
        method = "getLocation", at = @At("HEAD"), cancellable = true
    )
    protected void getLocation(final CallbackInfoReturnable<Vec3> cir) {

        cir.setReturnValue(new Vec3((double) ((TrackNodeLocation) (Object) this).getX() / 2,
            (double) ((TrackNodeLocation) (Object) this).getY() / 2,
            (double) ((TrackNodeLocation) (Object) this).getZ() / 2));
    }
}
