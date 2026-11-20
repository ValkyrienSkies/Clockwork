package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrShipPieces;

@Mixin(value = Entity.class, priority = 1100)
public abstract class MixinGateSeat {
    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
    private void clockwork$positionPassenger(Entity passenger, CallbackInfo ci) {
        var seat = GatemakyrShipPieces.seat(passenger, false, 1f);
        if (seat != null) passenger.setPos(seat.getFeet());
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void clockwork$seatEye(CallbackInfoReturnable<Vec3> cir) {
        var seat = GatemakyrShipPieces.seat((Entity)(Object)this, false, 1f);
        if (seat != null) cir.setReturnValue(seat.getEye());
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void clockwork$renderSeatEye(float pt, CallbackInfoReturnable<Vec3> cir) {
        Entity rider = (Entity)(Object)this;
        var seat = GatemakyrShipPieces.seat(rider, rider.level().isClientSide, pt);
        if (seat != null) cir.setReturnValue(seat.getEye());
    }
}
