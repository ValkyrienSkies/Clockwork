package org.valkyrienskies.clockwork.mixin.content.gatemakyr;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.valkyrienskies.mod.client.IVSCamera;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrShipPieces;

@Mixin(Camera.class)
public abstract class MixinGateSeatCamera {
    @Shadow protected abstract void setPosition(Vec3 point);

    @Inject(method = "setup", at = @At("TAIL"))
    private void clockwork$seatView(BlockGetter level, Entity rider, boolean detached, boolean reverse, float pt, CallbackInfo ci) {
        var seat = GatemakyrShipPieces.seat(rider, true, pt);
        if (seat == null) return;
        Camera camera = (Camera)(Object)this;
        Vec3 current = camera.getPosition();
        Vec3 offset = current.subtract(seat.getCanonicalEye());
        if (!detached) setPosition(seat.getEye());
        else if (offset.lengthSqr() < current.distanceToSqr(seat.getEye())) {
            var rotated = seat.getRotation().transform(new Vector3d(offset.x, offset.y, offset.z));
            setPosition(seat.getEye().add(rotated.x, rotated.y, rotated.z));
        }
        var rotation = seat.getRotation();
        if (rotation.x * rotation.x + rotation.y * rotation.y + rotation.z * rotation.z < 1e-12) return;
        var look = rotation.transform(new Vector3f(camera.getLookVector()));
        var left = rotation.transform(new Vector3f(camera.getLeftVector()));
        double horizontal = Math.hypot(look.x, look.z);
        double yaw = horizontal > 1e-5 ? Math.atan2(-look.x, look.z) : Math.atan2(left.z, left.x);
        double pitch = Math.atan2(-look.y, horizontal);
        double lateral = left.x * Math.cos(yaw) + left.z * Math.sin(yaw);
        double vertical = left.y * Math.cos(pitch) + (left.z * Math.cos(yaw) - left.x * Math.sin(yaw)) * Math.sin(pitch);
        ((IVSCamera)camera).setRotationVS((float)Math.toDegrees(yaw), (float)Math.toDegrees(pitch),
            (float)Math.toDegrees(Math.atan2(vertical, lateral)));
    }
}
