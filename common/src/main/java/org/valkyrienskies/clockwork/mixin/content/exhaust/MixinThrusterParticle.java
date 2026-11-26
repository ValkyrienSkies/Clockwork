package org.valkyrienskies.clockwork.mixin.content.exhaust;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlockEntity;

@Mixin(Particle.class)
abstract class MixinThrusterParticle {
    @Shadow @Final protected ClientLevel level;
    @Shadow protected double x, y, z, xd, yd, zd, xo, yo, zo;
    @Shadow public abstract void setPos(double x, double y, double z);
    @Shadow public abstract void remove();
    @Unique private boolean clockwork$exhaust;

    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDD)V", at = @At("RETURN"))
    private void clockwork$markExhaust(ClientLevel level, double x, double y, double z, CallbackInfo ci) {
        clockwork$exhaust = GasThrusterBlockEntity.isSpawningExhaust();
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void clockwork$sweepExhaust(double dx, double dy, double dz, CallbackInfo ci) {
        if (!clockwork$exhaust) return;
        GasThrusterBlockEntity.ExhaustMotion hit = GasThrusterBlockEntity.sweepExhaust(level, new Vec3(x, y, z), new Vec3(dx, dy, dz));
        if (hit == null) return;
        Vec3 p = hit.getPosition(), v = hit.getVelocity();
        setPos(p.x, p.y, p.z);
        if (hit.getCrossedGate()) { xo = p.x; yo = p.y; zo = p.z; }
        xd = v.x; yd = v.y; zd = v.z;
        if (hit.getStopped()) remove();
        ci.cancel();
    }
}
