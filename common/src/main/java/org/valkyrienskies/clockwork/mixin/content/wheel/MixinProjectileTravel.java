package org.valkyrienskies.clockwork.mixin.content.wheel;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireProjectileHits;

@Mixin(ServerLevel.class)
public class MixinProjectileTravel {
    @Inject(method = "tickNonPassenger", at = @At("RETURN"))
    private void clockwork$checkTireHit(Entity entity, CallbackInfo ci) {
        if (entity instanceof Projectile projectile)
            TireProjectileHits.check((ServerLevel) (Object) this, projectile);
    }
}
